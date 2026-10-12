package ar.arenasur;
import ar.arenasur.consola.Main;
import ar.arenasur.servicio.*;
import ar.arenasur.modelo.*;
import java.time.*;
import java.math.BigDecimal;

/** Verificaciones ejecutables sin bibliotecas externas. */
public final class Pruebas {
    private static final class RelojMutable extends Clock {
        private Instant instante;
        private final ZoneId zona;
        RelojMutable(Instant instante, ZoneId zona) { this.instante = instante; this.zona = zona; }
        void avanzar(Duration duracion) { instante = instante.plus(duracion); }
        @Override public ZoneId getZone() { return zona; }
        @Override public Clock withZone(ZoneId nuevaZona) { return new RelojMutable(instante,nuevaZona); }
        @Override public Instant instant() { return instante; }
    }
    private static void comprobar(boolean condicion, String caso) {
        if (!condicion) throw new AssertionError(caso);
        System.out.println("OK: " + caso);
    }
    private static void rechaza(Runnable accion, String caso) {
        try { accion.run(); } catch (IllegalArgumentException | NegocioException e) {
            System.out.println("OK: " + caso); return;
        }
        throw new AssertionError(caso);
    }
    public static void main(String[] args) {
        Clock fijo = Clock.fixed(Instant.parse("2026-10-12T15:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
        ReservaService s = Main.crearServicio(fijo);
        s.registrarCliente("Juan","Pérez","111","juan@example.com");
        LocalDateTime inicio = LocalDateTime.of(2026,10,20,18,0);
        comprobar(s.disponible(1,inicio,60), "Cancha inicialmente libre");
        Reserva r = s.registrar(1,1,inicio,60);
        comprobar(r.getSena().compareTo(new BigDecimal("12000.00")) == 0,"Seña del 20%");
        comprobar(r.getEstado() == EstadoReserva.PENDIENTE,"Reserva pendiente");
        Pago sena = s.registrarSena(r.getId());
        comprobar(sena.getImporte().compareTo(new BigDecimal("12000.00")) == 0,"Seña registrada");
        comprobar(r.getEstado() == EstadoReserva.CONFIRMADA,"Seña confirma reserva");
        comprobar(s.saldoPendiente(r.getId()).compareTo(new BigDecimal("48000.00")) == 0,"Saldo inicial");
        rechaza(() -> s.registrarSena(r.getId()),"No duplica seña");
        comprobar(s.pagos(r.getId()).size() == 1,"Consulta historial de pagos");
        comprobar(!s.disponible(1,inicio.plusMinutes(30),60),"Superposición parcial");
        comprobar(!s.disponible(1,inicio.minusMinutes(30),90),"Turno que contiene al existente");
        comprobar(s.disponible(1,inicio.plusMinutes(60),60),"Turnos contiguos permitidos");
        comprobar(s.disponible(2,inicio,60),"Otra cancha disponible");
        rechaza(() -> s.registrar(1,1,inicio,60),"Duplicado rechazado");
        rechaza(() -> s.registrar(99,1,inicio,60),"Cliente inexistente");
        rechaza(() -> s.disponible(99,inicio,60),"Cancha inexistente");
        rechaza(() -> s.disponible(1,inicio,45),"Duración inválida");
        rechaza(() -> s.disponible(1,LocalDateTime.of(2026,10,10,18,0),60),"Fecha pasada");
        rechaza(() -> s.registrarCliente("Otro","Cliente","111","JUAN@example.com"),"Email duplicado");
        Reserva vencida = s.registrar(1,1,inicio.plusDays(1),60);
        vencida.actualizarVencimiento(vencida.getVencimiento());
        comprobar(vencida.getEstado() == EstadoReserva.CANCELADA,"Vencimiento exacto cancela");
        comprobar(s.disponible(1,inicio.plusDays(1),60),"Turno vencido libera disponibilidad");
        Reserva anticipada = s.registrar(1,1,inicio.plusDays(2),60);
        s.registrarSena(anticipada.getId());
        s.cancelar(anticipada.getId());
        comprobar(s.pagos(anticipada.getId()).stream().anyMatch(p -> p.getTipo() == TipoPago.DEVOLUCION
            && p.getImporte().compareTo(new BigDecimal("12000.00")) == 0),"Cancelación anticipada devuelve seña");
        Reserva tardia = s.registrar(1,1,LocalDateTime.now(fijo).plusHours(1),60);
        s.registrarSena(tardia.getId());
        s.cancelar(tardia.getId());
        comprobar(s.pagos(tardia.getId()).stream().noneMatch(p -> p.getTipo() == TipoPago.DEVOLUCION),
            "Cancelación tardía retiene la seña si no hay saldo abonado");
        RelojMutable relojPagos = new RelojMutable(Instant.parse("2026-10-12T15:00:00Z"),
            ZoneId.of("America/Argentina/Buenos_Aires"));
        ReservaService flujoPagos = Main.crearServicio(relojPagos);
        flujoPagos.registrarCliente("Lucía","Gómez","333","lucia@example.com");
        LocalDateTime turnoPago = LocalDateTime.now(relojPagos).plusHours(5);
        Reserva reservaPago = flujoPagos.registrar(1,1,turnoPago,60);
        flujoPagos.registrarSena(reservaPago.getId());
        rechaza(() -> flujoPagos.registrarSaldo(reservaPago.getId()),
            "Saldo rechazado antes de finalizar el turno");
        relojPagos.avanzar(Duration.ofHours(5).plusMinutes(59));
        rechaza(() -> flujoPagos.registrarSaldo(reservaPago.getId()),
            "Saldo rechazado un minuto antes del fin");
        relojPagos.avanzar(Duration.ofMinutes(1));
        Pago pagoSaldo = flujoPagos.registrarSaldo(reservaPago.getId());
        comprobar(pagoSaldo.getImporte().compareTo(new BigDecimal("48000.00")) == 0,
            "Saldo habilitado al finalizar el turno");
        comprobar(flujoPagos.saldoPendiente(reservaPago.getId()).signum() == 0,
            "Saldo pendiente cero");
        rechaza(() -> flujoPagos.registrarSaldo(reservaPago.getId()),"No duplica saldo");
        comprobar(flujoPagos.pagos(reservaPago.getId()).size() == 2,"Consulta historial completo de pagos");
        flujoPagos.cancelar(reservaPago.getId());
        comprobar(flujoPagos.pagos(reservaPago.getId()).stream().anyMatch(p ->
            p.getTipo() == TipoPago.DEVOLUCION && p.getImporte().compareTo(new BigDecimal("48000.00")) == 0),
            "Cancelación tardía retiene seña y devuelve saldo");
        comprobar(s.canchas().get(2).obtenerTarifa(s.tarifas(),90).precio().compareTo(new BigDecimal("80000")) == 0,
            "Tarifa de pádel 90 minutos");
        RelojMutable relojLimite = new RelojMutable(Instant.parse("2026-10-12T15:00:00Z"),
            ZoneId.of("America/Argentina/Buenos_Aires"));
        ReservaService flujoVencimiento = Main.crearServicio(relojLimite);
        flujoVencimiento.registrarCliente("Eva","Díaz","222","eva@example.com");
        Reserva sinSena = flujoVencimiento.registrar(1,1,
            LocalDateTime.of(2026,10,20,18,0),60);
        relojLimite.avanzar(Duration.ofMinutes(30));
        rechaza(() -> flujoVencimiento.registrarSena(sinSena.getId()),"Seña rechazada al cumplirse el vencimiento");
        comprobar(sinSena.getEstado() == EstadoReserva.CANCELADA,"Vencimiento libera reserva sin seña");
        comprobar(flujoVencimiento.disponible(1,sinSena.getInicio(),60),"Horario disponible tras vencer reserva");

        RelojMutable relojModificacion = new RelojMutable(Instant.parse("2026-10-12T15:00:00Z"),
            ZoneId.of("America/Argentina/Buenos_Aires"));
        ReservaService flujoModificacion = Main.crearServicio(relojModificacion);
        flujoModificacion.registrarCliente("Ana","López","444","ana@example.com");
        LocalDateTime original = LocalDateTime.now(relojModificacion).plusDays(1).withHour(14);
        Reserva confirmada = flujoModificacion.registrar(1,1,original,60);
        flujoModificacion.registrarSena(confirmada.getId());
        LocalDateTime destino = original.plusDays(1).withHour(16);
        Reserva cambiada = flujoModificacion.modificar(confirmada.getId(),1,destino,60);
        comprobar(cambiada.getInicio().equals(destino) && cambiada.getEstado() == EstadoReserva.CONFIRMADA,
            "Modifica reserva confirmada y conserva el estado");
        flujoModificacion.modificar(confirmada.getId(),1,destino,90);
        comprobar(confirmada.getDuracion() == 90,
            "La reserva no se bloquea a sí misma al validar un cambio de duración");

        LocalDateTime exactamenteDosHoras = LocalDateTime.now(relojModificacion).plusHours(2);
        Reserva limiteModificable = flujoModificacion.registrar(1,2,exactamenteDosHoras,60);
        flujoModificacion.modificar(limiteModificable.getId(),2,exactamenteDosHoras.plusDays(1),60);
        comprobar(limiteModificable.getInicio().equals(exactamenteDosHoras.plusDays(1)),
            "Modificación permitida exactamente dos horas antes");

        ReservaService flujoLimiteCerrado = Main.crearServicio(relojModificacion);
        flujoLimiteCerrado.registrarCliente("Teo","Sol","888","teo@example.com");
        LocalDateTime menosDeDosHoras = LocalDateTime.now(relojModificacion).plusHours(1).plusMinutes(59);
        Reserva fueraDePlazo = flujoLimiteCerrado.registrar(1,1,menosDeDosHoras,60);
        rechaza(() -> flujoLimiteCerrado.modificar(fueraDePlazo.getId(),1,menosDeDosHoras.plusDays(1),60),
            "Rechaza modificación con menos de dos horas");
        comprobar(fueraDePlazo.getInicio().equals(menosDeDosHoras),
            "Rechazo por plazo conserva el turno original");

        ReservaService flujoPlazo = Main.crearServicio(relojModificacion);
        flujoPlazo.registrarCliente("Nora","Díaz","555","nora@example.com");
        Reserva pendienteModificable = flujoPlazo.registrar(1,1,
            LocalDateTime.now(relojModificacion).plusDays(3),60);
        relojModificacion.avanzar(Duration.ofMinutes(10));
        LocalDateTime vencimientoEsperado = LocalDateTime.now(relojModificacion).plusMinutes(30);
        flujoPlazo.modificar(pendienteModificable.getId(),1,pendienteModificable.getInicio().plusDays(1),60);
        comprobar(pendienteModificable.getEstado() == EstadoReserva.PENDIENTE
            && pendienteModificable.getVencimiento().equals(vencimientoEsperado),
            "Modificar reserva pendiente reinicia vencimiento por 30 minutos");
        relojModificacion.avanzar(Duration.ofMinutes(30));
        flujoPlazo.reservas();
        comprobar(pendienteModificable.getEstado() == EstadoReserva.CANCELADA,
            "Reserva pendiente modificada vence al cumplirse el nuevo plazo");

        RelojMutable relojAumento = new RelojMutable(Instant.parse("2026-10-12T15:00:00Z"),
            ZoneId.of("America/Argentina/Buenos_Aires"));
        ReservaService flujoAumento = Main.crearServicio(relojAumento);
        flujoAumento.registrarCliente("Leo","Paz","666","leo@example.com");
        Reserva reservaAumento = flujoAumento.registrar(1,1,
            LocalDateTime.now(relojAumento).plusDays(1).withHour(18),60);
        flujoAumento.registrarSena(reservaAumento.getId());
        LocalDateTime nuevoTurnoAumento = reservaAumento.getInicio().plusDays(1);
        flujoAumento.modificar(reservaAumento.getId(),1,nuevoTurnoAumento,90);
        comprobar(flujoAumento.diferenciaSenaPendiente(reservaAumento.getId())
            .compareTo(new BigDecimal("6000.00")) == 0,"Nueva seña superior deja diferencia de $6.000 pendiente");
        comprobar(flujoAumento.saldoPendiente(reservaAumento.getId())
            .compareTo(new BigDecimal("72000.00")) == 0,
            "El saldo se informa separado de la diferencia de seña, sin duplicarla");
        comprobar(reservaAumento.getEstado() == EstadoReserva.CONFIRMADA,
            "Diferencia pendiente conserva reserva confirmada");
        relojAumento.avanzar(Duration.between(LocalDateTime.now(relojAumento),
            reservaAumento.getFin()).plusMinutes(1));
        rechaza(() -> flujoAumento.registrarSaldo(reservaAumento.getId()),
            "No permite pagar saldo mientras falta completar la seña");
        Pago diferencia = flujoAumento.registrarSena(reservaAumento.getId());
        comprobar(diferencia.getImporte().compareTo(new BigDecimal("6000.00")) == 0,
            "Registra el pago adicional de seña por la diferencia exacta");
        Pago saldoAumento = flujoAumento.registrarSaldo(reservaAumento.getId());
        comprobar(saldoAumento.getImporte().compareTo(new BigDecimal("72000.00")) == 0,
            "Saldo recalculado para nueva tarifa de 90 minutos");
        comprobar(flujoAumento.saldoPendiente(reservaAumento.getId()).signum() == 0,
            "Seña y saldo completados luego de modificar tarifa");

        RelojMutable relojCredito = new RelojMutable(Instant.parse("2026-10-12T15:00:00Z"),
            ZoneId.of("America/Argentina/Buenos_Aires"));
        ReservaService flujoCredito = Main.crearServicio(relojCredito);
        flujoCredito.registrarCliente("Mía","Rey","777","mia@example.com");
        Reserva reservaCredito = flujoCredito.registrar(1,1,
            LocalDateTime.now(relojCredito).plusDays(1).withHour(18),90);
        flujoCredito.registrarSena(reservaCredito.getId());
        flujoCredito.modificar(reservaCredito.getId(),1,reservaCredito.getInicio().plusDays(1),60);
        comprobar(flujoCredito.creditoFavor(reservaCredito.getId()).compareTo(new BigDecimal("6000.00")) == 0,
            "Nueva seña inferior genera crédito de $6.000");
        comprobar(flujoCredito.saldoPendiente(reservaCredito.getId()).compareTo(new BigDecimal("42000.00")) == 0,
            "El crédito se refleja una sola vez en el saldo pendiente");
        relojCredito.avanzar(Duration.between(LocalDateTime.now(relojCredito),
            reservaCredito.getFin()).plusMinutes(1));
        Pago saldoConCredito = flujoCredito.registrarSaldo(reservaCredito.getId());
        comprobar(saldoConCredito.getImporte().compareTo(new BigDecimal("42000.00")) == 0,
            "El cobro final aplica el crédito sin duplicarlo");

        Reserva ocupadaBase = flujoCredito.registrar(1,2,
            LocalDateTime.now(relojCredito).plusDays(4).withHour(18),60);
        flujoCredito.registrar(1,2,ocupadaBase.getInicio().plusDays(1),60);
        LocalDateTime destinoOcupado = ocupadaBase.getInicio().plusDays(1);
        rechaza(() -> flujoCredito.modificar(ocupadaBase.getId(),2,destinoOcupado,60),
            "Rechaza modificar a un horario ocupado");
        comprobar(ocupadaBase.getInicio().equals(LocalDateTime.now(relojCredito).plusDays(4).withHour(18)),
            "Horario ocupado no produce cambios parciales");

        flujoCredito.cancelar(ocupadaBase.getId());
        rechaza(() -> flujoCredito.modificar(ocupadaBase.getId(),2,destinoOcupado.plusDays(2),60),
            "No permite modificar reserva cancelada");

        Clock relojConsultas = Clock.fixed(Instant.parse("2026-10-12T15:00:00Z"),
            ZoneId.of("America/Argentina/Buenos_Aires"));
        ReservaService consultas = Main.crearServicio(relojConsultas);
        consultas.registrarCliente("Juan","Pérez","111","juan@example.com");
        consultas.registrarCliente("María","López","222","maria@example.com");
        Reserva consultaConfirmada = consultas.registrar(1,1,LocalDateTime.of(2026,10,21,18,0),60);
        consultas.registrarSena(consultaConfirmada.getId());
        Reserva consultaPendiente = consultas.registrar(1,2,LocalDateTime.of(2026,10,20,18,0),60);
        Reserva consultaCancelada = consultas.registrar(2,1,LocalDateTime.of(2026,10,20,17,0),60);
        consultas.cancelar(consultaCancelada.getId());
        comprobar(consultas.consultarReservas(1L,null,null,null).size() == 2,
            "Consulta CU07 por cliente");
        comprobar(consultas.consultarReservas(null,2L,null,null).size() == 1,
            "Consulta CU07 por cancha");
        var porFecha = consultas.consultarReservas(null,null,LocalDate.of(2026,10,20),null);
        comprobar(porFecha.size() == 2 && porFecha.get(0).getId() == consultaCancelada.getId()
            && porFecha.get(1).getId() == consultaPendiente.getId(),
            "Consulta CU07 por fecha ordenada cronológicamente");
        comprobar(consultas.consultarReservas(null,null,null,EstadoReserva.CANCELADA).size() == 1,
            "Consulta CU07 por estado");
        comprobar(consultas.consultarReservas(1L,null,LocalDate.of(2026,10,20),null).size() == 1,
            "Combina filtros CU07 mediante conjunción");
        comprobar(consultas.consultarReservas(null,2L,LocalDate.of(2026,10,20),EstadoReserva.PENDIENTE)
            .size() == 1,"Combina cancha, fecha y estado en CU07");
        comprobar(consultas.consultarReservas(null,null,LocalDate.of(2026,10,22),null).isEmpty(),
            "Consulta CU07 sin coincidencias");
        rechaza(() -> consultas.consultarReservas(null,null,null,null),
            "CU07 exige al menos un criterio");
        int cantidadPagosAntesDetalle = consultas.pagos(consultaConfirmada.getId()).size();
        EstadoReserva estadoAntesDetalle = consultaConfirmada.getEstado();
        Reserva detalleConsulta = consultas.detalleReserva(consultaConfirmada.getId());
        comprobar(detalleConsulta.getId() == consultaConfirmada.getId()
            && detalleConsulta.getCliente().getNombreCompleto().equals("Juan Pérez")
            && detalleConsulta.getCancha().getNombre().equals("Fútbol 1")
            && detalleConsulta.getDuracion() == 60
            && detalleConsulta.getPrecioTotal().compareTo(new BigDecimal("60000.00")) == 0,
            "Detalle CU07 presenta datos principales de la reserva");
        comprobar(consultaConfirmada.getEstado() == estadoAntesDetalle
            && consultas.pagos(consultaConfirmada.getId()).size() == cantidadPagosAntesDetalle,
            "Detalle CU07 no modifica reserva ni pagos");
        rechaza(() -> consultas.detalleReserva(999L),"Detalle CU07 rechaza ID inexistente");

        RelojMutable relojAgenda = new RelojMutable(Instant.parse("2026-10-12T15:00:00Z"),
            ZoneId.of("America/Argentina/Buenos_Aires"));
        ReservaService agendaService = Main.crearServicio(relojAgenda);
        agendaService.registrarCliente("Pablo","Díaz","111","pablo@example.com");
        LocalDate fechaAgenda = LocalDate.of(2026,10,20);
        Reserva agendaPendiente = agendaService.registrar(1,1,fechaAgenda.atTime(10,0),60);
        Reserva agendaConfirmada = agendaService.registrar(1,1,fechaAgenda.atTime(14,0),90);
        agendaService.registrarSena(agendaConfirmada.getId());
        Reserva agendaCancelada = agendaService.registrar(1,2,fechaAgenda.atTime(11,0),60);
        agendaService.cancelar(agendaCancelada.getId());
        int pagosAntesAgenda = agendaService.pagos(agendaConfirmada.getId()).size();
        AgendaDiaria agenda = agendaService.consultarAgenda(fechaAgenda,null,null);
        AgendaCancha agendaFutbol1 = agenda.canchas().stream()
            .filter(a -> a.cancha().getId() == 1).findFirst().orElseThrow();
        comprobar(agenda.canchas().size() == 5 && agendaFutbol1.ocupaciones().size() == 2
            && agendaFutbol1.ocupaciones().get(0).getId() == agendaPendiente.getId()
            && agendaFutbol1.ocupaciones().get(1).getId() == agendaConfirmada.getId(),
            "CU10 muestra pendientes vigentes y confirmadas ordenadas por cancha y horario");
        comprobar(agenda.canchas().stream().filter(a -> a.cancha().getId() == 2)
            .findFirst().orElseThrow().ocupaciones().isEmpty(),
            "CU10 excluye reservas canceladas de las ocupaciones");
        comprobar(agendaFutbol1.franjasDisponibles().stream().filter(f -> f.duracion() == 60).count() == 3
            && agendaFutbol1.franjasDisponibles().stream().filter(f -> f.duracion() == 90).count() == 2,
            "CU10 calcula inicios disponibles según duración y reservas ocupadas");
        comprobar(agendaFutbol1.franjasDisponibles().stream().filter(f -> f.duracion() == 60)
            .anyMatch(f -> f.desde().equals(fechaAgenda.atTime(15,30))
                && f.hasta().equals(fechaAgenda.atTime(22,0))),
            "CU10 conserva como último inicio disponible las 22:00");
        comprobar(agendaService.consultarAgenda(fechaAgenda,1L,null).canchas().size() == 1,
            "CU10 filtra por cancha");
        comprobar(agendaService.consultarAgenda(fechaAgenda,null,"pÁdel").canchas().size() == 2,
            "CU10 filtra por deporte sin distinguir mayúsculas");
        rechaza(() -> agendaService.consultarAgenda(fechaAgenda,null,"Hockey"),
            "CU10 rechaza deporte inexistente");
        comprobar(agendaService.pagos(agendaConfirmada.getId()).size() == pagosAntesAgenda
            && agendaConfirmada.getEstado() == EstadoReserva.CONFIRMADA,
            "CU10 no modifica pagos ni reservas vigentes");
        AgendaDiaria agendaVacia = agendaService.consultarAgenda(fechaAgenda.plusDays(1),null,null);
        comprobar(agendaVacia.canchas().size() == 5
            && agendaVacia.canchas().stream().allMatch(a -> a.ocupaciones().isEmpty()
                && a.franjasDisponibles().size() == 2),
            "CU10 sin reservas informa agenda libre para las cinco canchas");

        comprobar(agendaService.disponible(1,fechaAgenda.atTime(9,0),60),
            "CU10 permite iniciar a las 09:00");
        comprobar(agendaService.disponible(2,fechaAgenda.atTime(22,0),90),
            "CU10 permite iniciar a las 22:00 y terminar a las 23:30");
        rechaza(() -> agendaService.disponible(3,fechaAgenda.atTime(8,59),60),
            "CU10 rechaza inicios anteriores a las 09:00");
        rechaza(() -> agendaService.disponible(3,fechaAgenda.atTime(22,1),60),
            "CU10 rechaza inicios posteriores a las 22:00");

        RelojMutable relojAgendaVencida = new RelojMutable(Instant.parse("2026-10-12T15:00:00Z"),
            ZoneId.of("America/Argentina/Buenos_Aires"));
        ReservaService agendaVencidaService = Main.crearServicio(relojAgendaVencida);
        agendaVencidaService.registrarCliente("Sol","Vega","222","sol@example.com");
        Reserva pendienteVencidaAgenda = agendaVencidaService.registrar(1,1,
            fechaAgenda.atTime(10,0),60);
        relojAgendaVencida.avanzar(Duration.ofMinutes(30));
        AgendaDiaria agendaTrasVencimiento = agendaVencidaService.consultarAgenda(fechaAgenda,1L,null);
        comprobar(pendienteVencidaAgenda.getEstado() == EstadoReserva.CANCELADA
            && agendaTrasVencimiento.canchas().get(0).ocupaciones().isEmpty()
            && agendaTrasVencimiento.canchas().get(0).franjasDisponibles().size() == 2,
            "CU10 cancela pendiente vencida y libera su horario");
    }
}

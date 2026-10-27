/*
 * Cyphail - Graph Query Engine Prototype
 * EIF400-II-2026 - Escuela de Informatica, UNA
 * Grupo: G05
 * Autores: Luis Felipe Jimenez Fernandez, Jose David Chavarria Villalobos,
 * Jostin Jimenez Alfaro, Angel Rojas Ruano
 */

% Pruebas plunit del engine de Cyphail.
% Se corren con: swipl -g run_tests -t halt src/test/resources/prolog/cyphail_engine_tests.pl
%
% :- style_check(-singleton) porque las variables __A, __E1, etc. se repiten
% entre distintos tests de este archivo; por MQI ese aviso no aparece.
:- style_check(-singleton).

:- prolog_load_context(directory, Dir), assertz(cy_test_dir(Dir)).
:- consult('../../../main/resources/prolog/cyphail_engine.pl').


:- begin_tests(cy_comparadores).

test(eq_numeros_valor) :- cy_eq(30, 30.0).
test(eq_atomos_identicos) :- cy_eq('Juan Perez', 'Juan Perez').
test(eq_atomos_distintos, [fail]) :- cy_eq('Juan', 'Maria').
test(neq_numeros) :- cy_neq(1, 2).
test(neq_falla_si_son_iguales, [fail]) :- cy_neq(5, 5).
test(gt_numeros) :- cy_gt(43, 21).
test(gt_falla_sin_error_si_no_son_numeros, [fail]) :- cy_gt(abc, 3).
test(lt_numeros) :- cy_lt(5, 10).
test(gte_igual) :- cy_gte(30, 30).
test(lte_igual) :- cy_lte(30, 30).

:- end_tests(cy_comparadores).


:- begin_tests(cy_formato_rows).

test(cell_texto_simple) :-
    cy_cell('Juan Perez', C), C == "\"Juan Perez\"".

test(cell_numero) :-
    cy_cell(30, C), C == "\"30\"".

test(cell_con_comillas) :-
    cy_cell('Ana "la" Perez', C), C == "\"Ana \\\"la\\\" Perez\"".

test(cell_con_barra_invertida) :-
    cy_cell('a\\b', C), C == "\"a\\\\b\"".

test(format_rows_ejemplo_del_contrato) :-
    cy_format_rows(['a.name', 'b.name'], [['Juan Perez', 'Maria Salas']], Rows),
    Rows == "a.name b.name\n\"Juan Perez\" \"Maria Salas\"".

test(format_rows_sin_filas_solo_encabezado) :-
    cy_format_rows(['a.name'], [], Rows), Rows == "a.name".

test(format_rows_sin_columnas_es_vacio) :-
    cy_format_rows([], [[algo]], Rows), Rows == "".

test(format_rows_celda_vacia) :-
    cy_format_rows(['x'], [['']], Rows), Rows == "x\n\"\"".

:- end_tests(cy_formato_rows).


:- begin_tests(cy_ciclo_consulta).

% MATCH (a)-[:amigo]->(b), (b)-[:amigo]->(a) WHERE a.age > 18 AND a.age = b.age RETURN a.name, b.name
test(mutuos_mayores_de_18) :-
    cy_create_query(Id, (query_mutuos(NA, NB) :-
        user_amigos_edge(E1, amigo, A, B),
        user_amigos_edge(E2, amigo, B, A),
        cy_neq(E1, E2),
        user_amigos_prop(A, age, AgeA),
        cy_gt(AgeA, 18),
        user_amigos_prop(B, age, AgeB),
        cy_eq(AgeA, AgeB),
        user_amigos_prop(A, name, NA),
        user_amigos_prop(B, name, NB))),
    cy_exec_query(Id, ['a.name', 'b.name'], Rows),
    Rows == "a.name b.name\n\"Juan Perez\" \"Maria Salas\"\n\"Maria Salas\" \"Juan Perez\"",
    cy_erase_query(Id).

% MATCH (a)-[:amigo]->(b) WHERE a.age > b.age RETURN a.name, b.name
test(mayor_que_su_amigo) :-
    cy_create_query(Id, (query_mayor(NA, NB) :-
        user_amigos_edge(_, amigo, A, B),
        user_amigos_prop(A, age, AgeA),
        user_amigos_prop(B, age, AgeB),
        cy_gt(AgeA, AgeB),
        user_amigos_prop(A, name, NA),
        user_amigos_prop(B, name, NB))),
    cy_exec_query(Id, ['a.name', 'b.name'], Rows),
    Rows == "a.name b.name\n\"Juan Perez\" \"Ana Flores\"\n\"Jorge Soto\" \"Juan Perez\"",
    cy_erase_query(Id).

% Mutuos con a.age > 40 AND a.age = b.age -> 0 filas: solo el encabezado
test(mutuos_mayores_de_40_sin_resultados) :-
    cy_create_query(Id, (query_vacia(NA, NB) :-
        user_amigos_edge(E1, amigo, A, B),
        user_amigos_edge(E2, amigo, B, A),
        cy_neq(E1, E2),
        user_amigos_prop(A, age, AgeA),
        cy_gt(AgeA, 40),
        user_amigos_prop(B, age, AgeB),
        cy_eq(AgeA, AgeB),
        user_amigos_prop(A, name, NA),
        user_amigos_prop(B, name, NB))),
    cy_exec_query(Id, ['a.name'], Rows),
    Rows == "a.name",
    cy_erase_query(Id).

% MATCH (a:Person) WHERE a.age > 30 RETURN a.name AS nombre
test(mayores_de_30_con_alias) :-
    cy_create_query(Id, (query_alias(Nombre) :-
        user_amigos_label(A, 'Person'),
        user_amigos_prop(A, age, Age),
        cy_gt(Age, 30),
        user_amigos_prop(A, name, Nombre))),
    cy_exec_query(Id, [nombre], Rows),
    Rows == "nombre\n\"Jorge Soto\"",
    cy_erase_query(Id).

% Cabeza sin argumentos (CREATE sin RETURN): sin columnas -> Rows vacio
test(cabeza_sin_argumentos_da_rows_vacio) :-
    cy_create_query(Id, (query_sin_return :- true)),
    cy_exec_query(Id, [], Rows),
    Rows == "",
    cy_erase_query(Id).

test(erase_borra_la_clausula) :-
    cy_create_query(Id, (query_borrar :- true)),
    cy_erase_query(Id),
    \+ cy_stored(Id, _).

:- end_tests(cy_ciclo_consulta).


:- begin_tests(cy_no_escribe_stdout).

% Levanta swipl igual que lo hara el broker de Jostin (mqi_start con
% write_connection_values) y confirma que SOLO imprime puerto y contrasena.
test(mqi_solo_imprime_puerto_y_contrasena) :-
    cy_test_dir(Dir),
    atomic_list_concat([Dir, '/../../../main/resources/prolog/cyphail_engine.pl'], File0),
    absolute_file_name(File0, File),
    tmp_file_stream(text, SalidaArchivo, SalidaFlujo),
    close(SalidaFlujo),
    open(SalidaArchivo, write, FlujoEscritura),
    process_create(path(swipl),
                    ['-l', File, '-g', 'mqi_start', '-t', 'halt',
                     '--', '--write_connection_values=true'],
                    [stdout(stream(FlujoEscritura)), stderr(null), process(Pid)]),
    cy_esperar_dos_lineas(SalidaArchivo, 40, Lineas),
    process_kill(Pid, kill),
    process_wait(Pid, _),
    close(FlujoEscritura),
    Lineas = [Linea1, Linea2],
    atom_number(Linea1, _Puerto),
    string_length(Linea2, LargoContrasena),
    LargoContrasena > 0.

cy_esperar_dos_lineas(_Archivo, 0, _) :- !, fail.
cy_esperar_dos_lineas(Archivo, Intentos, Lineas) :-
    read_file_to_string(Archivo, Contenido, []),
    split_string(Contenido, "\n", "", Partes),
    exclude(==(""), Partes, Lineas0),
    ( Lineas0 = [_, _ | _]
    -> Lineas = Lineas0
    ;  sleep(0.05),
       Intentos1 is Intentos - 1,
       cy_esperar_dos_lineas(Archivo, Intentos1, Lineas)
    ).

:- end_tests(cy_no_escribe_stdout).
// Massa de dados inicial para desenvolvimento.
// Executado automaticamente pelo container do MongoDB na primeira inicialização.
db = db.getSiblingDB('matriculadb');

db.matriculas.insertMany([
  {
    alunoId: 'ALU-001',
    businessKey: 'GRAD/ENG/BACH/PRESENCIAL/NOTURNO/UNIT-SP-01',
    status: 'ATIVA',
    turma: {
      codigo: 'T2026-001',
      diasDaSemana: ['SEGUNDA', 'QUARTA'],
      horarioInicio: '19:00',
      horarioFim: '22:30'
    },
    cicloId: NumberLong(20261),
    dataMatricula: ISODate('2026-02-10T08:00:00Z')
  },
  {
    alunoId: 'ALU-002',
    businessKey: 'GRAD/ENG/BACH/PRESENCIAL/NOTURNO/UNIT-SP-01',
    status: 'ATIVA',
    turma: {
      codigo: 'T2026-001',
      diasDaSemana: ['SEGUNDA', 'QUARTA', 'SEXTA'],
      horarioInicio: '19:00',
      horarioFim: '22:30'
    },
    cicloId: NumberLong(20261),
    dataMatricula: ISODate('2026-02-12T09:00:00Z')
  },
  {
    alunoId: 'ALU-003',
    businessKey: 'GRAD/ENG/BACH/PRESENCIAL/NOTURNO/UNIT-SP-01',
    status: 'CANCELADA',
    turma: {
      codigo: 'T2026-001',
      diasDaSemana: ['SEGUNDA', 'QUARTA'],
      horarioInicio: '19:00',
      horarioFim: '22:30'
    },
    cicloId: NumberLong(20261),
    dataMatricula: ISODate('2026-02-10T10:00:00Z')
  },
  {
    alunoId: 'ALU-004',
    businessKey: 'GRAD/ADM/BACH/EAD/NOTURNO/UNIT-RJ-02',
    status: 'ATIVA',
    turma: {
      codigo: 'T2026-050',
      diasDaSemana: ['TERÇA', 'QUINTA'],
      horarioInicio: '19:00',
      horarioFim: '22:30'
    },
    cicloId: NumberLong(20261),
    dataMatricula: ISODate('2026-02-11T14:00:00Z')
  }
]);

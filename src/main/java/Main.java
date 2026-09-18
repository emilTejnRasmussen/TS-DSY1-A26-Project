import repository.PostgresTraceabilityRepository;

void main() throws Exception
{
    PostgresTraceabilityRepository repository =
            new PostgresTraceabilityRepository();

    IO.println(repository.findAnimalRegistrationNumbersByProductId(1));

    IO.println(repository.findProductIdsByAnimalRegistrationNumber("A-1003"));

}
package main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.javatuples.Octet;
import org.javatuples.Triplet;

/* Author: Jo�o Luiz Grave Gross (@jlggross)
 * Github: https://github.com/jlggross/MEC-simulator
 * Dependencies: javatuples-1.2.jar
 *
 * General info:
 * - The systems cycle time is 1 micro second.
 * - The simulation checks the entire system and advances 1 micro second in time.  
 * - All decision making process is made in intervals of 1 micro second.
 * - The simulation ends when all tasks are finalized. 
 * 
 * Times:
 * - All times used in the variables are in micro seconds (the base time unit for the simulation)
 * */


public class SimulatorExecution {

	static int CORE_FREE = 1;
	static int CORE_OCCUPIED = 2;
	
	static int POLICY1_IOT = 1;
	static int POLICY2_MEC = 2;
	static int POLICY3_CLOUD = 3;
	
	static int TASK_ALIVE = 1;
	static int TASK_CONCLUDED = 2;	
	static int TASK_CANCELED = 3;
	
		
	public static void main(String args[]) throws IOException {
		
		// ---------------------------------------------------------------------------
		// 1. Setting simulation variables
		// * Application
		// * Number of tasks
		// * Number of IoT Devices
		// * Number of MEC Servers
		// * Energy and time coefficients for task cost calculation
		// * Alpha, beta and gamma coefficients for task cost calculation
		// ---------------------------------------------------------------------------
		
		// Applications used in the simulation 
		List<Application> appList = new ArrayList<Application>();
				
		// Application 1
		long taskGenerationRate = (long) (10 * Math.pow(10, 4)); 
		long taskDataEntrySize = (long) (36.288 * 8 * Math.pow(10, 6));
		long taskResultSize = (long) Math.pow(10, 4);
		long computacionalLoadCPUcycles = (long) (20 * Math.pow(10, 4));
		long deadlineCriticalTasks = (long) (0.5 * Math.pow(10, 6)); 
		double percentageOfCriticalTasks = (double) 0.1;
		appList.add(new Application("App1", taskGenerationRate, taskDataEntrySize, taskResultSize, 
				computacionalLoadCPUcycles, percentageOfCriticalTasks, deadlineCriticalTasks));
		
		// Application 2
		taskGenerationRate = (long) (0.1 * Math.pow(10, 6)); 
		taskDataEntrySize = (long) (4 * 8 * Math.pow(10, 6));
		taskResultSize = (long) (5 * Math.pow(10, 3));
		computacionalLoadCPUcycles = (long) (200 * Math.pow(10, 6));
		deadlineCriticalTasks = (long) (0.1 * Math.pow(10, 6)); 
		percentageOfCriticalTasks = (double) 0.5;
		//appList.add(new Application("App2", taskGenerationRate, taskDataEntrySize, taskResultSize, computacionalLoadCPUcycles, percentageOfCriticalTasks, deadlineCriticalTasks));
		
		// Lists for number of tasks, IoT devices and MEC servers
		List<Integer> listNumberOfTasks = Arrays.asList(500, 5000);
		List<Integer> listNumberIoTDevices = Arrays.asList(100, 500, 1000);
		List<Integer> listNumberMECServers = Arrays.asList(1, 2);
		
		for(int numberTasks : listNumberOfTasks) {
			List<Task> listRunningTasks = new ArrayList<Task>();
			Task[] listFinishedTasks = new Task[numberTasks];
		for(int numberIoTDevices : listNumberIoTDevices) {
			if(numberIoTDevices > numberTasks)
				continue;
		for(int numberMECServers : listNumberMECServers) { 
		for(Application app : appList) {
			app.setQtdeTarefas(numberTasks); // Define number of tasks that will be created
			
			// Set coefficients to calculate the tasks cost in each allocation option
			double coefficientEnergy, coefficientTime;
			coefficientEnergy = 4.0 / 5.0;
			coefficientTime = 1 - coefficientEnergy;
			
			// Set the coefficients used in the tasks minimization equation
			double alpha, beta, gamma;
			alpha = beta = gamma = 1.0 / 3.0;
			
			// ---------------------------------------------------------------------------
			// 2. Create entities for the architecture
			// * Creates the IoT Devices
			// * Creates the MEC Servers
			// * A DataCenter entity is created my the Scheduler to access the Cloud
			// characteristics. Cloud is meant to have virtual infinite resources.
			// ---------------------------------------------------------------------------
			long rateOfGeneratedTasks = app.getTaxaGeracao();
			
			IoTDevice[] listOfIoTDevices = new IoTDevice[numberIoTDevices];
			for(int i = 0; i < numberIoTDevices; i++)
				listOfIoTDevices[i] = new IoTDevice("Device-" + i, rateOfGeneratedTasks);
			
			MECServer[] listOfMECServers = new MECServer[numberMECServers];
			for(int i = 0; i < numberMECServers; i++)
				listOfMECServers[i] = new MECServer("MEC-" + i);
			
			// ---------------------------------------------------------------------------
			// 3. Initializes simulation control variables
			// ---------------------------------------------------------------------------	
			long systemTime = 0; // Initializes simulation time (starts in zero) 
			int numberTasksCanceledAndConcluded = 0;
			int numberCreatedTasks = 0;
			printMessageOnConsole("LoadVariationExperiment " + numberTasks + "-" + numberIoTDevices + "-" + numberMECServers + "-" + (long) app.getCargaComputacional());

			// ---------------------------------------------------------------------------
			// 4. Initiates simulation
			// ---------------------------------------------------------------------------
			while(Boolean.TRUE) {
				
				// ---------------------------------------------------------------------------				
				// 5. Verify if there are tasks to be created
				// ---------------------------------------------------------------------------
				for(int i = 0; i < numberIoTDevices; i++) {
					if(((systemTime - listOfIoTDevices[i].getTempobase()) % app.getTaxaGeracao()) == 0) {
						
						// A task is created
						Task newTask = new Task("TarefaDummy", "DeviceDummy", -1, 0, 0, 0, 0);
						if(numberCreatedTasks < numberTasks) {
							if(app.defineSeTarefaCritica(numberCreatedTasks) == Boolean.TRUE) {
								newTask = new Task("Task-" + numberCreatedTasks, listOfIoTDevices[i].getId(), 
													app.getDeadlineCriticas(), systemTime, app.getCargaComputacional(),
													app.getEntradaDados(), app.getResultados());
							}
							else {
								newTask = new Task("Task-" + numberCreatedTasks, listOfIoTDevices[i].getId(), -1, 
													systemTime, app.getCargaComputacional(), app.getEntradaDados(), app.getResultados());
							}
							numberCreatedTasks++;
						}
						else
							break;
						
						// ---------------------------------------------------------------------------
						// 5.1. Start the scheduler and task allocation
						// ---------------------------------------------------------------------------
						
						// Computes allocation options costs for the task
						Scheduler scheduler = new Scheduler(newTask, coefficientEnergy, coefficientTime, alpha, beta, gamma); 
						//scheduler.imprimeCustos();
						
						// Verify if the IoT device that created the task is with it's processing core available
						boolean flagIoTDevice = Boolean.FALSE;
						if(listOfIoTDevices[i].verificaCPULivre() == Boolean.TRUE)
							flagIoTDevice = Boolean.TRUE;
						
						// Verify is there is an avaliable processing core in the MEC servers
						boolean flagMECServer = Boolean.FALSE;
						for(int j = 0; j < numberMECServers; j++) {
							if(listOfMECServers[j].verificaCPULivre() == Boolean.TRUE) {
								flagMECServer = Boolean.TRUE;
								break;
							}
						}
							
						// Searches for the smallest cost and then allocate the task
						Octet<Double, Double, Double, Double, Double, Long, Double, Integer> octet;
						octet = scheduler.defineMenorCusto(flagIoTDevice, flagMECServer);

						// Occupy hardware resources
						if(octet.getValue7() == POLICY1_IOT) {
							listOfIoTDevices[i].alteraStatusCPU(CORE_OCCUPIED);
							listOfIoTDevices[i].consomeBateria(octet.getValue1() + octet.getValue2());
						} else if(octet.getValue7() == POLICY2_MEC) {
							for(int j = 0; j < numberMECServers; j++) {
								if(listOfMECServers[j].verificaCPULivre() == Boolean.TRUE) {
									listOfMECServers[j].OcupaCPU();
									break;
								}
							}
						}
						
						// -----------------------------------------------------------------------------
						// 5.2. Updates energy consumed, elapsed time and allocation policy for the task
						// -----------------------------------------------------------------------------						
						newTask.setEnergiaExecucao(octet.getValue1());
						newTask.setEnergiaTransmissaoDados(octet.getValue2());
						newTask.setTempoExecucao(octet.getValue3());
						newTask.setTempoTransmissaoDados(octet.getValue4());
						newTask.setPolitica(octet.getValue7());
						
						if(Boolean.FALSE) {
							printMessageOnConsole(listOfIoTDevices[i].getId() + " - Battery Level: " + 
									listOfIoTDevices[i].getNivelBateria() + "; CORE Free: " + 
									listOfIoTDevices[i].verificaCPULivre());
						}
											
						
						// -----------------------------------------------------------------------------
						// 5.3. Task is allocated. Add task in the monitoring list. Start new iteration
						// -----------------------------------------------------------------------------
						listRunningTasks.add(newTask);
						
						if(Boolean.FALSE) {
							// Print the number of occupied CPU cores in the MEC servers
							printMessageOnConsole(newTask.getIdTarefa() + " created; System time: " + systemTime);
							for(int j = 0; j < numberMECServers; j++) {
								int qtde = listOfMECServers[j].getQuantidadeCPUsLivres();
								printMessageOnConsole(listOfMECServers[j].getId() + " with " + qtde + " free CPU cores; System time: " + systemTime);
							}
						}
						
						// ---------------------------------------------------------------------------
						// Iteration end - Go back to 5.
						// ---------------------------------------------------------------------------
					}
				}
				
				// ---------------------------------------------------------------------------
				// 6. Verify if tasks are finished
				// ---------------------------------------------------------------------------
				if(!listRunningTasks.isEmpty()) {
					List<Task> listRunningTasksAux = new ArrayList<Task>();
					listRunningTasksAux.addAll(listRunningTasks);
					for(Task aux : listRunningTasksAux) {
						Task task = aux;
						
						if(task.verificaSeTarefaDeveFinalizar(systemTime) == Boolean.TRUE) {
							listFinishedTasks[numberTasksCanceledAndConcluded] = task;
							numberTasksCanceledAndConcluded++;
							listRunningTasks.remove(aux);
							
							// ---------------------------------------------------------------------------
							// 6.1. Free resources 
							// ---------------------------------------------------------------------------
							if(task.getPolitica() == POLICY1_IOT) {
								int id = Integer.parseInt(task.getIdDeviceGerador().split("-")[1]);
								listOfIoTDevices[id].alteraStatusCPU(CORE_FREE);
							}
							if(task.getPolitica() == POLICY2_MEC) {
								for(int j = 0; j < numberMECServers; j++) {
									if(listOfMECServers[j].LiberaCPU() == Boolean.TRUE)
										break;
								}
							}
								
							if(Boolean.TRUE) {
								if(numberTasksCanceledAndConcluded % 100 == 0) // Restrict prints
									printMessageOnConsole("Number of tasks concluded: " + numberTasksCanceledAndConcluded);
							}
						}
					}
				}
				
				// ---------------------------------------------------------------------------
				// 7. Verify if all tasks are concluded or canceled
				// ---------------------------------------------------------------------------
				if(numberTasksCanceledAndConcluded == numberTasks) {
					if(Boolean.FALSE) {
						for(int j = 0; j < numberTasks; j++) {
							System.out.println(listFinishedTasks[j].getIdTarefa() + "; Energia: " + listFinishedTasks[j].getEnergiaTotalConsumida());
						}
					}
					break; // Finishes simulation round
				}
								
				// Updates system time - advances 1 micro second in time
				systemTime++; 
			}	
			
			// ---------------------------------------------------------------------------
			// Simulation round ended - Print results for analysis
			// ---------------------------------------------------------------------------
			if(Boolean.TRUE) {
				String filename = "01-" + numberTasks + "-" + numberIoTDevices + "-" + numberMECServers + "-" + (long) app.getCargaComputacional();
				String testType = "LoadVariation";
				printSimulationLog(filename, listFinishedTasks, coefficientEnergy, coefficientTime, testType);
			}
		}
		}
		}
		}

	}
	
	
	/* Print message on console
	 * 
	 * */
	public static void printMessageOnConsole(String message) {
		System.out.println(message);
	}
	
		
	/* Print simulation log for analysis
	 * 
	 * TODO 
	 * 
	 * */
	public static void printSimulationLog(String filename, Task[] tarefasFinalizadas, 
			double fatorEnergia, double fatorTempo, String testType) throws IOException {
		
		filename = filename + "-" + testType + ".txt";
		
		// Octet<Tempo; Pol�tica; Status Finaliza��o, Energia CPU, Energia transmiss�o, Tempo CPU, Tempo transmiss�o, Custo>
		List<Octet<Long, String, String, Long, Long, Long, Long, Long>> listOctet = 
				new ArrayList<Octet<Long, String, String, Long, Long, Long, Long, Long>>();
		
		for(int i = 0; i < tarefasFinalizadas.length; i++) {
			String policy;
			if(tarefasFinalizadas[i].getPolitica() == POLICY1_IOT)
				policy = "POLICY1_IOT";
			else if(tarefasFinalizadas[i].getPolitica() == POLICY2_MEC)
				policy = "POLICY2_MEC";
			else
				policy = "POLICY3_CLOUD";
			
			String statusFinalizacao;
			if(tarefasFinalizadas[i].getStatusTarefa() == TASK_CONCLUDED)
				statusFinalizacao = "TASK_CONCLUDED";
			else
				statusFinalizacao = "TASK_CANCELED";
			
			Octet<Long, String, String, Long, Long, Long, Long, Long> octet = 
					new Octet<Long, String, String, Long, Long, Long, Long, Long>
					( 
						(long) (tarefasFinalizadas[i].getTempoBase() + tarefasFinalizadas[i].getTempoTotalDecorrido()),
						policy, 
						statusFinalizacao, 
						(long) tarefasFinalizadas[i].getEnergiaExecucao(), 
						(long) tarefasFinalizadas[i].getEnergiaTransmissaoDados(), 
						tarefasFinalizadas[i].getTempoExecucao(), 
						tarefasFinalizadas[i].getTempoTransmissaoDados(), 
						(long) (fatorEnergia*tarefasFinalizadas[i].getEnergiaTotalConsumida() + fatorTempo*tarefasFinalizadas[i].getTempoTotalDecorrido())
					);
			
			listOctet.add(octet);
		}
		
		// Order tuple list by the tasks finalization time
		listOctet.sort(null);
		
		String header = "Tempo;Politica;Status Finalizacao;Energia CPU;Energia Transmissoes;Tempo CPU;Tempo Transmissoes;Custo\n";
		imprimeOctetoParaArquivo(filename, header, listOctet);
	}
	
	
	/* Print Octet
	 * [Time; Policy; Finalization Status, Energy CPU core, Energy data transmission, Time CPU core, Time data transmission	, Cost]
	 * 
	 * 0 Time 							: Time in which the task was ended in the system.
	 * 1 Policy 						: Chosen allocation option/policy. Can be 1 (IoT, 2 (MEC) or 3 (Cloud).
	 * 2 Finalization Status			: Task finalization status. Can be concluded or canceled. 
	 * 3 Energy CPU core				: Dynamic energy consumed by the CPU core during execution.
	 * 4 Energy data transmission 		: Consumed energy for data transmissions
	 * 5 Time CPU core					: Elapsed time when executing the task in the CPU core
	 * 6 Time data transmission			: Elapsed time for data transmissions 
	 * 7 Cost							: Cost
	 * 
	 * */
	public static void imprimeOctetoParaArquivo(String filename, String header, 
			List<Octet<Long, String, String, Long, Long, Long, Long, Long>> listOctet) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
	    
		writer.write(header);
	    for(Octet<Long, String, String, Long, Long, Long, Long, Long> octet : listOctet) {
			writer.write(octet.getValue0() + ";" + octet.getValue1() + ";" + octet.getValue2() + ";" +
						 octet.getValue3() + ";" + octet.getValue4() + ";" + octet.getValue5() + ";" + 
						 octet.getValue6() + ";" + octet.getValue7() + "\n");
	    }
	    writer.close();
	}
	
}

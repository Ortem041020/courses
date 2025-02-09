import java.io.*;
import java.util.*;

class Employee {
    int id;
    String name;
    double salary;
    Integer managerId;

    public Employee(int id, String name, double salary, Integer managerId) {
        this.id = id;
        this.name = name;
        this.salary = salary;
        this.managerId = managerId;
    }

    public boolean isValid() {
        return salary > 0 && name != null && !name.isEmpty();
    }
}

class Manager extends Employee {
    String department;

    public Manager(int id, String name, double salary, String department) {
        super(id, name, salary, null);
        this.department = department;
    }
}

class Department {
    String name;
    Manager manager;
    List<Employee> employees = new ArrayList<>();

    public Department(String name, Manager manager) {
        this.name = name;
        this.manager = manager;
    }

    public void addEmployee(Employee employee) {
        employees.add(employee);
    }

    public double getAverageSalary() {
        double total = 0;
        for (Employee e : employees) {
            total = total + e.salary;
        }
        total += manager.salary;
        return total / getEmployeeCount();
    }

    public int getEmployeeCount() {
        return employees.size() + 1;
    }
}


public class EmployeeManager {
    private static Map<Integer, Manager> managers = new HashMap<>();
    private static Map<String, Department> departments = new TreeMap<>();
    private static Set<String> invalidData = new HashSet<>();

    public static void main(String[] args) {
        String sortField = "name";
        String order = "asc";
        String output = "console";
        String path = null;

        for (String arg : args) {
            if (arg.startsWith("--sort=") || arg.startsWith("-s=")) {
                sortField = arg.split("=")[1];
            }
            if (arg.startsWith("--order=") || arg.startsWith("-o=desc")) {
                order = arg.split("=")[1];
            }
            if (arg.startsWith("--output=") || arg.startsWith("-o=file")) {
                output = arg.split("=")[1];
            }
            if (arg.startsWith("--path=")) {
                path = arg.split("=")[1];
            }
        }

        if (!"name".equals(sortField) && !"salary".equals(sortField)) {
            System.out.println("Ошибка: Некорректное значение для --sort. Допустимые значения: name, salary.");
            return;
        }

        if (!"asc".equals(order) && !"desc".equals(order)) {
            System.out.println("Ошибка: Некорректное значение для --order. Допустимые значения: asc, desc.");
            return;
        }

        if ("file".equals(output)) {
            if (path == null) {
                System.out.println("Ошибка: Для --output=file должен быть задан путь с флагом --path.");
                return;
            }
        } else if (path != null) {
            System.out.println("Ошибка: --path не может быть задан без --output=file.");
            return;
        }

        String inputFilePath = "inputFile.txt";
        List<String> lines = readFile(inputFilePath);

        for (String line : lines) {
            processLine(line);
        }

        for (Department department : departments.values()) {
            sortEmployees(department, sortField, order);
        }
        if ("file".equals(output) && path != null) {
            writeToFile(path);
        } else {
            writeToConsole();
        }

        if (!invalidData.isEmpty()) {
            if ("console".equals(output)) {
                System.out.println("Некорректные данные:");
                for (String data : invalidData) {
                    System.out.println(data);
                }
            } else if ("file".equals(output) && path != null) {
                writeInvalidDataToFile(path);
            }
        }
    }

    private static List<String> readFile(String filePath) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line.trim());
            }
        } catch (IOException e) {
            System.out.println("Ошибка при чтении файла: " + e.getMessage());
        }
        return lines;
    }

    private static void processLine(String line) {
        String[] parts = line.split(",");
        if (parts.length != 5) {
            invalidData.add(line);
            return;
        }

        String type = parts[0].trim();
        try {
            if ("Manager".equals(type)) {
                int id = Integer.parseInt(parts[1].trim());
                String name = parts[2].trim();
                double salary = Double.parseDouble(parts[3].trim());
                String department = parts[4].trim();

                if (salary >= 0) {
                    Manager manager = new Manager(id, name, salary, department);
                    managers.put(id, manager);
                    departments.putIfAbsent(department, new Department(department, manager));
                } else {
                    invalidData.add(line);
                }
            } else if ("Employee".equals(type)) {
                int id = Integer.parseInt(parts[1].trim());
                String name = parts[2].trim();
                double salary = parseSalary(parts[3].trim());
                int managerId = Integer.parseInt(parts[4].trim());

                if (salary >= 0) {
                    Employee employee = new Employee(id, name, salary, managerId);
                    if (managers.containsKey(managerId)) {
                        departments.get(managers.get(managerId).department).addEmployee(employee);
                    } else {
                        invalidData.add(line);
                    }
                } else {
                    invalidData.add(line);
                }
            } else {
                invalidData.add(line);
            }
        } catch (Exception e) {
            invalidData.add(line);
        }
    }

    private static double parseSalary(String salaryStr) {
        if (salaryStr.isEmpty()) {
            return 0;
        }

        try {
            double salary = Double.parseDouble(salaryStr);
            if (salary < 0) {
                return -1;
            }
            return salary;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void sortEmployees(Department department, String sortField, String order) {
        Comparator<Employee> comparator = null;

        if ("salary".equals(sortField)) {
            comparator = Comparator.comparingDouble(e -> e.salary);
        } else if ("name".equals(sortField)) {
            comparator = Comparator.comparing(e -> e.name);
        }

        if (comparator != null) {
            if ("desc".equals(order)) {
                comparator = comparator.reversed();
            }
            department.employees.sort(comparator);
        }
    }

    private static void writeInvalidDataToFile(String path) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, true))) {
            writer.newLine();
            writer.write("Некорректные данные:");
            writer.newLine();
            for (String data : invalidData) {
                writer.write(data);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Ошибка при записи в файл: " + e.getMessage());
        }
    }

    private static void writeToConsole() {
        for (Department department : departments.values()) {
            System.out.println(department.name);
            System.out.println("Manager," + department.manager.id + "," + department.manager.name + "," + department.manager.salary);
            for (Employee e : department.employees) {
                System.out.println("Employee," + e.id + "," + e.name + "," + e.salary);
            }
            System.out.printf("%d, %.2f\n", department.getEmployeeCount(), department.getAverageSalary());
        }

    }
    private static void writeToFile(String path) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            for (Department department : departments.values()) {
                writer.write(department.name);
                writer.newLine();
                writer.write("Manager," + department.manager.id + "," + department.manager.name + "," + department.manager.salary);
                writer.newLine();
                for (Employee e : department.employees) {
                    writer.write("Employee," + e.id + "," + e.name + "," + e.salary);
                    writer.newLine();
                }
                writer.write(department.getEmployeeCount() + ", " + department.getAverageSalary());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Ошибка при записи в файл: " + e.getMessage());
        }
    }
}

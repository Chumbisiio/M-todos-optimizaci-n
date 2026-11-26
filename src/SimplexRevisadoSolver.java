import java.util.ArrayList;
import java.util.List;

public final class SimplexRevisadoSolver {
    private static final double EPSILON = 1e-9;
    private static final double FEASIBILITY_EPSILON = 1e-7;
    private static final int MAX_ITERATIONS = 10000;

    public enum Relation {
        LESS_OR_EQUAL,
        GREATER_OR_EQUAL,
        EQUAL
    }

    public enum Status {
        OPTIMAL,
        INFEASIBLE,
        UNBOUNDED
    }

    public static final class Constraint {
        private final double[] coefficients;
        private final Relation relation;
        private final double rhs;

        public Constraint(double[] coefficients, Relation relation, double rhs) {
            if (coefficients == null) {
                throw new IllegalArgumentException("Los coeficientes de la restricción no pueden ser nulos.");
            }
            this.coefficients = coefficients.clone();
            this.relation = relation;
            this.rhs = rhs;
        }
    }

    public static final class Result {
        private final Status status;
        private final double[] variableValues;
        private final double[] constraintSlacks;
        private final double objectiveValue;
        private final boolean multipleOptima;
        private final boolean degenerate;

        private Result(Status status, double[] variableValues, double[] constraintSlacks,
                double objectiveValue, boolean multipleOptima, boolean degenerate) {
            this.status = status;
            this.variableValues = variableValues;
            this.constraintSlacks = constraintSlacks;
            this.objectiveValue = objectiveValue;
            this.multipleOptima = multipleOptima;
            this.degenerate = degenerate;
        }

        public Status getStatus() {
            return status;
        }

        public double[] getVariableValues() {
            return variableValues.clone();
        }

        public double[] getConstraintSlacks() {
            return constraintSlacks.clone();
        }

        public double getObjectiveValue() {
            return objectiveValue;
        }

        public boolean hasMultipleOptima() {
            return multipleOptima;
        }

        public boolean isDegenerate() {
            return degenerate;
        }
    }

    private SimplexRevisadoSolver() {
    }

    public static Result solve(double[] objective, boolean maximize, List<Constraint> constraints) {
        validateInput(objective, constraints);
        int variableCount = objective.length;
        int rowCount = constraints.size();

        List<double[]> columns = new ArrayList<>();
        for (int variable = 0; variable < variableCount; variable++) {
            double[] column = new double[rowCount];
            for (int row = 0; row < rowCount; row++) {
                Constraint constraint = constraints.get(row);
                column[row] = (constraint.rhs < 0 ? -1 : 1) * constraint.coefficients[variable];
            }
            columns.add(column);
        }

        double[] rhs = new double[rowCount];
        int[] basis = new int[rowCount];
        List<Boolean> artificialColumns = new ArrayList<>();
        for (int variable = 0; variable < variableCount; variable++) {
            artificialColumns.add(false);
        }

        for (int row = 0; row < rowCount; row++) {
            Constraint constraint = constraints.get(row);
            double sign = constraint.rhs < 0 ? -1 : 1;
            rhs[row] = sign * constraint.rhs;
            Relation relation = sign < 0 ? reverse(constraint.relation) : constraint.relation;

            if (relation == Relation.LESS_OR_EQUAL) {
                basis[row] = addUnitColumn(columns, artificialColumns, row, 1, false);
            } else if (relation == Relation.GREATER_OR_EQUAL) {
                addUnitColumn(columns, artificialColumns, row, -1, false);
                basis[row] = addUnitColumn(columns, artificialColumns, row, 1, true);
            } else {
                basis[row] = addUnitColumn(columns, artificialColumns, row, 1, true);
            }
        }

        boolean[] artificial = new boolean[artificialColumns.size()];
        for (int column = 0; column < artificial.length; column++) {
            artificial[column] = artificialColumns.get(column);
        }

        double[] phaseOneCosts = new double[columns.size()];
        for (int column = 0; column < phaseOneCosts.length; column++) {
            phaseOneCosts[column] = artificial[column] ? -1 : 0;
        }
        Optimization phaseOne = optimize(columns, rhs, basis, phaseOneCosts, artificial, false);
        if (phaseOne.status == Status.UNBOUNDED) {
            throw new IllegalStateException("La primera fase del simplex no debe ser no acotada.");
        }
        if (phaseOne.objectiveValue < -FEASIBILITY_EPSILON) {
            return new Result(Status.INFEASIBLE, new double[variableCount], new double[rowCount],
                    Double.NaN, false, false);
        }

        double[] phaseTwoCosts = new double[columns.size()];
        for (int variable = 0; variable < variableCount; variable++) {
            phaseTwoCosts[variable] = maximize ? objective[variable] : -objective[variable];
        }
        Optimization phaseTwo = optimize(columns, rhs, phaseOne.basis, phaseTwoCosts, artificial, true);
        if (phaseTwo.status == Status.UNBOUNDED) {
            return new Result(Status.UNBOUNDED, new double[variableCount], new double[rowCount],
                    Double.NaN, false, false);
        }

        double[] values = new double[variableCount];
        for (int row = 0; row < rowCount; row++) {
            int basicColumn = phaseTwo.basis[row];
            if (basicColumn < variableCount) {
                values[basicColumn] = clean(phaseTwo.basicValues[row]);
            }
        }

        double[] slacks = new double[rowCount];
        for (int row = 0; row < rowCount; row++) {
            Constraint constraint = constraints.get(row);
            double activity = dot(constraint.coefficients, values);
            double slack = constraint.relation == Relation.LESS_OR_EQUAL
                    ? constraint.rhs - activity
                    : constraint.relation == Relation.GREATER_OR_EQUAL ? activity - constraint.rhs : 0;
            slacks[row] = clean(slack);
        }

        double objectiveValue = dot(objective, values);
        return new Result(Status.OPTIMAL, values, slacks, clean(objectiveValue),
                phaseTwo.multipleOptima, phaseTwo.degenerate);
    }

    private static Optimization optimize(List<double[]> columns, double[] rhs, int[] startingBasis,
            double[] costs, boolean[] artificial, boolean phaseTwo) {
        int rowCount = rhs.length;
        int[] basis = startingBasis.clone();

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            double[][] basisMatrix = basisMatrix(columns, basis);
            double[] basicValues = solveLinearSystem(basisMatrix, rhs);
            for (int row = 0; row < rowCount; row++) {
                if (basicValues[row] < -FEASIBILITY_EPSILON) {
                    throw new IllegalStateException("La base del simplex perdió factibilidad numérica.");
                }
                if (basicValues[row] < 0) {
                    basicValues[row] = 0;
                }
            }

            double[] basicCosts = new double[rowCount];
            for (int row = 0; row < rowCount; row++) {
                basicCosts[row] = costs[basis[row]];
            }
            double[] dual = solveLinearSystem(transpose(basisMatrix), basicCosts);
            boolean[] isBasic = new boolean[columns.size()];
            for (int basicColumn : basis) {
                isBasic[basicColumn] = true;
            }

            int entering = -1;
            boolean multipleOptima = false;
            for (int column = 0; column < columns.size(); column++) {
                if (isBasic[column] || (phaseTwo && artificial[column])) {
                    continue;
                }
                double reducedCost = costs[column] - dot(dual, columns.get(column));
                if (reducedCost > EPSILON) {
                    entering = column;
                    break;
                }
                if (Math.abs(reducedCost) <= EPSILON && !artificial[column]) {
                    multipleOptima = true;
                }
            }

            if (entering < 0) {
                double value = 0;
                for (int row = 0; row < rowCount; row++) {
                    value += basicCosts[row] * basicValues[row];
                }
                boolean degenerate = false;
                for (double basicValue : basicValues) {
                    if (basicValue <= FEASIBILITY_EPSILON) {
                        degenerate = true;
                        break;
                    }
                }
                return new Optimization(Status.OPTIMAL, basis, basicValues, value,
                        multipleOptima, degenerate);
            }

            double[] direction = solveLinearSystem(basisMatrix, columns.get(entering));
            int leavingRow = -1;
            double minimumRatio = Double.POSITIVE_INFINITY;
            for (int row = 0; row < rowCount; row++) {
                if (direction[row] <= EPSILON) {
                    continue;
                }
                double ratio = basicValues[row] / direction[row];
                if (ratio < minimumRatio - EPSILON
                        || (Math.abs(ratio - minimumRatio) <= EPSILON
                                && (leavingRow < 0 || basis[row] < basis[leavingRow]))) {
                    minimumRatio = ratio;
                    leavingRow = row;
                }
            }
            if (leavingRow < 0) {
                return new Optimization(Status.UNBOUNDED, basis, basicValues,
                        Double.POSITIVE_INFINITY, false, false);
            }
            basis[leavingRow] = entering;
        }

        throw new IllegalStateException("El simplex excedió el límite de iteraciones.");
    }

    private static void validateInput(double[] objective, List<Constraint> constraints) {
        if (objective == null || objective.length == 0) {
            throw new IllegalArgumentException("Debe ingresar al menos una variable.");
        }
        if (constraints == null) {
            throw new IllegalArgumentException("La lista de restricciones no puede ser nula.");
        }
        for (double coefficient : objective) {
            requireFinite(coefficient, "Los coeficientes del objetivo deben ser números finitos.");
        }
        for (Constraint constraint : constraints) {
            if (constraint == null || constraint.coefficients.length != objective.length) {
                throw new IllegalArgumentException("Las restricciones deben tener un coeficiente por variable.");
            }
            if (constraint.relation == null) {
                throw new IllegalArgumentException("Cada restricción debe tener una relación válida.");
            }
            requireFinite(constraint.rhs, "Los lados derechos deben ser números finitos.");
            for (double coefficient : constraint.coefficients) {
                requireFinite(coefficient, "Los coeficientes de las restricciones deben ser números finitos.");
            }
        }
    }

    private static void requireFinite(double value, String message) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private static Relation reverse(Relation relation) {
        if (relation == Relation.LESS_OR_EQUAL) {
            return Relation.GREATER_OR_EQUAL;
        }
        if (relation == Relation.GREATER_OR_EQUAL) {
            return Relation.LESS_OR_EQUAL;
        }
        return Relation.EQUAL;
    }

    private static int addUnitColumn(List<double[]> columns, List<Boolean> artificialColumns,
            int row, double value, boolean artificial) {
        double[] column = new double[columns.get(0).length];
        column[row] = value;
        columns.add(column);
        artificialColumns.add(artificial);
        return columns.size() - 1;
    }

    private static double[][] basisMatrix(List<double[]> columns, int[] basis) {
        double[][] matrix = new double[basis.length][basis.length];
        for (int column = 0; column < basis.length; column++) {
            double[] values = columns.get(basis[column]);
            for (int row = 0; row < basis.length; row++) {
                matrix[row][column] = values[row];
            }
        }
        return matrix;
    }

    private static double[] solveLinearSystem(double[][] matrix, double[] values) {
        int size = values.length;
        double[][] augmented = new double[size][size + 1];
        for (int row = 0; row < size; row++) {
            System.arraycopy(matrix[row], 0, augmented[row], 0, size);
            augmented[row][size] = values[row];
        }

        for (int column = 0; column < size; column++) {
            int pivotRow = column;
            for (int row = column + 1; row < size; row++) {
                if (Math.abs(augmented[row][column]) > Math.abs(augmented[pivotRow][column])) {
                    pivotRow = row;
                }
            }
            if (Math.abs(augmented[pivotRow][column]) <= 1e-12) {
                throw new IllegalStateException("La matriz base del simplex es singular.");
            }
            double[] temporary = augmented[column];
            augmented[column] = augmented[pivotRow];
            augmented[pivotRow] = temporary;

            double pivot = augmented[column][column];
            for (int entry = column; entry <= size; entry++) {
                augmented[column][entry] /= pivot;
            }
            for (int row = 0; row < size; row++) {
                if (row == column) {
                    continue;
                }
                double factor = augmented[row][column];
                for (int entry = column; entry <= size; entry++) {
                    augmented[row][entry] -= factor * augmented[column][entry];
                }
            }
        }

        double[] solution = new double[size];
        for (int row = 0; row < size; row++) {
            solution[row] = augmented[row][size];
        }
        return solution;
    }

    private static double[][] transpose(double[][] matrix) {
        double[][] transposed = new double[matrix.length][matrix.length];
        for (int row = 0; row < matrix.length; row++) {
            for (int column = 0; column < matrix.length; column++) {
                transposed[row][column] = matrix[column][row];
            }
        }
        return transposed;
    }

    private static double dot(double[] left, double[] right) {
        double value = 0;
        for (int index = 0; index < left.length; index++) {
            value += left[index] * right[index];
        }
        return value;
    }

    private static double clean(double value) {
        return Math.abs(value) <= EPSILON ? 0 : value;
    }

    private static final class Optimization {
        private final Status status;
        private final int[] basis;
        private final double[] basicValues;
        private final double objectiveValue;
        private final boolean multipleOptima;
        private final boolean degenerate;

        private Optimization(Status status, int[] basis, double[] basicValues, double objectiveValue,
                boolean multipleOptima, boolean degenerate) {
            this.status = status;
            this.basis = basis;
            this.basicValues = basicValues;
            this.objectiveValue = objectiveValue;
            this.multipleOptima = multipleOptima;
            this.degenerate = degenerate;
        }
    }
}

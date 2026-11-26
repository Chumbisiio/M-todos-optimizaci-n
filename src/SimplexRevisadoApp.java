import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

public class SimplexRevisadoApp {
    private static final Color NAVY = new Color(28, 48, 74);
    private static final Color BLUE = new Color(43, 105, 173);
    private static final Color PALE_BLUE = new Color(235, 243, 251);
    private static final Color MUTED = new Color(102, 116, 132);
    private static final Color BORDER = new Color(220, 227, 234);

    private final DefaultTableModel constraintsModel = new DefaultTableModel(
            new Object[][] {
                {"2", "1", "<=", "100"},
                {"1", "3", "<=", "90"},
                {"0", "1", "<=", "40"}
            },
            new Object[] {"x1", "x2", "Relación", "Lado derecho"}) {
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    };

    private final JTable constraintsTable = new JTable(constraintsModel);
    private final JLabel statusLabel = new JLabel(" Por: Juan Esteban Agudelo, Jerónimo Betancur, Juan Manuel Ocpampo ");
    private final JComboBox<String> objectiveType = new JComboBox<>(new String[] {"Maximizar", "Minimizar"});
    private final javax.swing.JTextField objectiveCoefficients = textField("50, 80");
    private final JLabel stateValue = new JLabel("Sin resolver");
    private final JLabel objectiveValue = new JLabel("—");
    private final JLabel variablesValue = new JLabel("x1: —     x2: —");
    private final JLabel solutionDetails = new JLabel(
            "<html><div style='text-align:center'>La solución óptima y las holguras o excesos<br>"
                    + "de cada restricción aparecerán en esta sección.</div></html>",
            SwingConstants.CENTER);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SimplexRevisadoApp().show());
    }

    private void show() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo cargar el estilo del sistema.", exception);
        }

        JFrame frame = new JFrame("Simplex Revisado | Programación Lineal");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1000, 680));
        frame.setContentPane(buildContent());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(Color.WHITE);
        content.add(buildHeader(), BorderLayout.NORTH);

        JPanel workspace = new JPanel(new BorderLayout(16, 0));
        workspace.setBackground(new Color(246, 248, 251));
        workspace.setBorder(BorderFactory.createEmptyBorder(18, 20, 12, 20));
        workspace.add(buildModelPanel(), BorderLayout.WEST);
        workspace.add(buildResultsPanel(), BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout());
        body.add(workspace, BorderLayout.CENTER);
        body.add(buildStatusBar(), BorderLayout.SOUTH);
        content.add(body, BorderLayout.CENTER);
        return content;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel title = new JLabel("Simplex Revisado");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        header.add(title, BorderLayout.WEST);
        return header;
    }

    private JPanel buildModelPanel() {
        JPanel panel = cardPanel();
        panel.setPreferredSize(new Dimension(430, 0));
        panel.setLayout(new BorderLayout(0, 12));
        panel.add(sectionHeading("Problema", "Ingresar datos"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 0, 5, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        addFormRow(form, c, 0, "Objetivo", objectiveType);
        addFormRow(form, c, 1, "Coeficientes de Z", objectiveCoefficients);
        addFormRow(form, c, 2, "Variables", new JLabel("x1, x2  (no negativas)"));

        JLabel constraintsLabel = new JLabel("Restricciones");
        constraintsLabel.setForeground(NAVY);
        constraintsLabel.setFont(constraintsLabel.getFont().deriveFont(Font.BOLD, 13f));
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.insets = new Insets(14, 0, 4, 0);
        form.add(constraintsLabel, c);

        constraintsTable.setRowHeight(30);
        constraintsTable.setFillsViewportHeight(true);
        constraintsTable.setShowVerticalLines(false);
        constraintsTable.getTableHeader().setReorderingAllowed(false);
        constraintsTable.getColumnModel().getColumn(2).setCellEditor(
                new javax.swing.DefaultCellEditor(new JComboBox<>(new String[] {"<=", ">=", "="})));
        JScrollPane tableScroll = new JScrollPane(constraintsTable);
        tableScroll.setPreferredSize(new Dimension(385, 150));
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        c.gridy = 4;
        c.insets = new Insets(0, 0, 2, 0);
        form.add(tableScroll, c);

        JButton addConstraint = new JButton("+ Agregar restricción");
        addConstraint.addActionListener(event -> constraintsModel.addRow(new Object[] {"", "", "<=", ""}));
        JButton removeConstraint = new JButton("Quitar seleccionada");
        removeConstraint.addActionListener(event -> {
            int row = constraintsTable.getSelectedRow();
            if (row >= 0) {
                constraintsModel.removeRow(row);
            }
        });
        JPanel rowActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        rowActions.setOpaque(false);
        rowActions.add(addConstraint);
        rowActions.add(removeConstraint);
        c.gridy = 5;
        c.insets = new Insets(4, 0, 8, 0);
        form.add(rowActions, c);

        addFormRow(form, c, 6, "Modo de solución", new JLabel("Solución directa"));

        JButton solveButton = new JButton("Resolver modelo");
        solveButton.setBackground(BLUE);
        solveButton.setForeground(Color.WHITE);
        solveButton.setFocusPainted(false);
        solveButton.addActionListener(event -> solveProblem());
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = 2;
        c.insets = new Insets(14, 0, 0, 0);
        form.add(solveButton, c);

        panel.add(form, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildResultsPanel() {
        JPanel panel = cardPanel();
        panel.setLayout(new BorderLayout(0, 12));
        panel.add(sectionHeading("Resultados", "Resultado directo del modelo."), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Resumen", buildSummaryTab());
        tabs.addTab("Gráfica", buildPlaceholderTab("Región factible y vértices (disponible para dos variables)."));
        tabs.addTab("Sensibilidad", buildPlaceholderTab("Precios sombra, costos reducidos y rangos."));
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSummaryTab() {
        JPanel summary = new JPanel(new BorderLayout(0, 12));
        summary.setBackground(Color.WHITE);
        summary.setBorder(BorderFactory.createEmptyBorder(16, 12, 12, 12));

        JPanel metrics = new JPanel(new GridBagLayout());
        metrics.setOpaque(false);
        addMetric(metrics, 0, "Estado", stateValue);
        addMetric(metrics, 1, "Valor de Z", objectiveValue);
        addMetric(metrics, 2, "Variables", variablesValue);
        summary.add(metrics, BorderLayout.NORTH);

        solutionDetails.setForeground(MUTED);
        summary.add(solutionDetails, BorderLayout.CENTER);
        return summary;
    }

    private JPanel buildPlaceholderTab(String description) {
        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.setBackground(Color.WHITE);
        placeholder.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel label = new JLabel("<html><div style='text-align:center'>" + description
                + "<br><br><span style='color:#667484'>Sección reservada para una etapa posterior.</span></div></html>",
                SwingConstants.CENTER);
        label.setForeground(NAVY);
        placeholder.add(label, BorderLayout.CENTER);
        return placeholder;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(246, 248, 251));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 22, 12, 22));
        statusLabel.setForeground(MUTED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(12f));
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    private void addFormRow(JPanel form, GridBagConstraints c, int row, String label, Component field) {
        JLabel caption = new JLabel(label);
        caption.setForeground(MUTED);
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        c.insets = new Insets(6, 0, 6, 12);
        form.add(caption, c);
        c.gridx = 1;
        c.weightx = 1;
        c.insets = new Insets(6, 0, 6, 0);
        form.add(field, c);
    }

    private void addMetric(JPanel panel, int row, String name, JLabel valueLabel) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = row;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 4, 0, 4);

        JPanel metric = new JPanel(new BorderLayout(0, 8));
        metric.setBackground(PALE_BLUE);
        metric.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JLabel title = new JLabel(name);
        title.setForeground(MUTED);
        valueLabel.setForeground(NAVY);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 15f));
        metric.add(title, BorderLayout.NORTH);
        metric.add(valueLabel, BorderLayout.CENTER);
        panel.add(metric, c);
    }

    private JPanel sectionHeading(String title, String subtitle) {
        JPanel heading = new JPanel(new BorderLayout(0, 5));
        heading.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(NAVY);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 17f));
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(MUTED);
        subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(12f));
        heading.add(titleLabel, BorderLayout.NORTH);
        heading.add(subtitleLabel, BorderLayout.SOUTH);
        return heading;
    }

    private JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        return panel;
    }

    private javax.swing.JTextField textField(String value) {
        javax.swing.JTextField field = new javax.swing.JTextField(value);
        field.setPreferredSize(new Dimension(210, 30));
        return field;
    }

    private void solveProblem() {
        try {
            double[] objective = parseCoefficients(objectiveCoefficients.getText(), 2, "coeficientes de Z");
            java.util.List<SimplexRevisadoSolver.Constraint> constraints = new java.util.ArrayList<>();
            for (int row = 0; row < constraintsModel.getRowCount(); row++) {
                double[] coefficients = new double[2];
                for (int column = 0; column < coefficients.length; column++) {
                    Object cell = constraintsModel.getValueAt(row, column);
                    coefficients[column] = parseNumber(cell, "coeficiente de la restricción " + (row + 1));
                }
                Object relationCell = constraintsModel.getValueAt(row, 2);
                if (relationCell == null) {
                    throw new IllegalArgumentException("Seleccione una relación para la restricción " + (row + 1) + ".");
                }
                SimplexRevisadoSolver.Relation relation;
                String relationText = relationCell.toString().trim();
                if ("<=".equals(relationText)) {
                    relation = SimplexRevisadoSolver.Relation.LESS_OR_EQUAL;
                } else if (">=".equals(relationText)) {
                    relation = SimplexRevisadoSolver.Relation.GREATER_OR_EQUAL;
                } else if ("=".equals(relationText)) {
                    relation = SimplexRevisadoSolver.Relation.EQUAL;
                } else {
                    throw new IllegalArgumentException("Relación inválida en la restricción " + (row + 1) + ".");
                }
                double rhs = parseNumber(constraintsModel.getValueAt(row, 3),
                        "lado derecho de la restricción " + (row + 1));
                constraints.add(new SimplexRevisadoSolver.Constraint(coefficients, relation, rhs));
            }

            boolean maximize = objectiveType.getSelectedIndex() == 0;
            SimplexRevisadoSolver.Result result =
                    SimplexRevisadoSolver.solve(objective, maximize, constraints);
            displayResult(result);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            JOptionPane.showMessageDialog(null, exception.getMessage(), "No se pudo resolver",
                    JOptionPane.ERROR_MESSAGE);
            statusLabel.setText(" Revise los datos del modelo.");
        }
    }

    private void displayResult(SimplexRevisadoSolver.Result result) {
        if (result.getStatus() == SimplexRevisadoSolver.Status.INFEASIBLE) {
            setSummary("Infactible", "—", "—", "El modelo no tiene solución factible.");
            statusLabel.setText(" El modelo no tiene solución factible.");
            return;
        }
        if (result.getStatus() == SimplexRevisadoSolver.Status.UNBOUNDED) {
            setSummary("No acotado", "—", "—", "La función objetivo puede mejorar sin límite.");
            statusLabel.setText(" La solución no está acotada.");
            return;
        }

        double[] variables = result.getVariableValues();
        String state = result.hasMultipleOptima() ? "Óptimo múltiple" : "Óptimo";
        if (result.isDegenerate()) {
            state += " (degenerado)";
        }
        String variableText = String.format("x1: %s     x2: %s", format(variables[0]), format(variables[1]));
        StringBuilder details = new StringBuilder("<html><div style='text-align:center'>");
        double[] slacks = result.getConstraintSlacks();
        for (int row = 0; row < slacks.length; row++) {
            String kind = "<=".equals(constraintsModel.getValueAt(row, 2))
                    ? "Holgura" : ">=".equals(constraintsModel.getValueAt(row, 2)) ? "Exceso" : "Igualdad";
            details.append(kind).append(" restricción ").append(row + 1).append(": ")
                    .append(format(slacks[row])).append("<br>");
        }
        details.append("</div></html>");
        setSummary(state, format(result.getObjectiveValue()), variableText, details.toString());
        statusLabel.setText(" Solución calculada con simplex revisado.");
    }

    private void setSummary(String state, String objective, String variables, String details) {
        stateValue.setText(state);
        objectiveValue.setText(objective);
        variablesValue.setText(variables);
        solutionDetails.setText(details);
    }

    private double[] parseCoefficients(String text, int expectedCount, String fieldName) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Ingrese los " + fieldName + ".");
        }
        String[] parts = text.trim().split("\\s*,\\s*");
        if (parts.length != expectedCount) {
            throw new IllegalArgumentException("Ingrese exactamente " + expectedCount
                    + " valores para los " + fieldName + ", separados por comas.");
        }
        double[] values = new double[parts.length];
        for (int index = 0; index < parts.length; index++) {
            values[index] = parseNumber(parts[index], fieldName);
        }
        return values;
    }

    private double parseNumber(Object value, String fieldName) {
        if (value == null || value.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Ingrese el " + fieldName + ".");
        }
        try {
            double number = Double.parseDouble(value.toString().trim());
            if (Double.isNaN(number) || Double.isInfinite(number)) {
                throw new NumberFormatException();
            }
            return number;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("El " + fieldName + " debe ser un número válido.");
        }
    }

    private String format(double value) {
        return String.format("%.6f", value).replaceAll("\\.?0+$", "");
    }
}

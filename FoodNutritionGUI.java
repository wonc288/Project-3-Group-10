import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class FoodNutritionGUI {
    private JFrame frame;
    private DefaultListModel<String> foodListModel;
    private JList<String> foodList;
    private JLabel totalLabel, recommendationLabel;
    private JTextArea addedFoodsArea;
    private JTextField weightField;
    private JComboBox<String> activityLevelBox;
    private Map<String, double[]> foodData;
    private Map<String, Integer> foodQuantities;
    private double totalProtein = 0, totalCarbs = 0, totalFat = 0, totalCalories = 0;
    private double userWeight = 70;
    private String activityLevel = "Moderate";

    public FoodNutritionGUI() {
        showTitleScreen();
    }

    private void showTitleScreen() {
        JFrame titleFrame = new JFrame("Welcome");
        titleFrame.setSize(300, 300);
        titleFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        titleFrame.setLayout(new BorderLayout(5, 5));

        JLabel titleLabel = new JLabel("Food & Exercise Tracker", SwingConstants.CENTER);
        JButton startButton = new JButton("Start");

        // Panel for weight and activity level input
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        inputPanel.add(new JLabel("Enter your weight (kg):"));
        weightField = new JTextField("70");
        inputPanel.add(weightField);
        inputPanel.add(new JLabel("Select activity level:"));
        String[] activityLevels = {"Sedentary", "Light", "Moderate", "Active", "Very Active"};
        activityLevelBox = new JComboBox<>(activityLevels);
        inputPanel.add(activityLevelBox);

        startButton.addActionListener(e -> {
            try {
                userWeight = Double.parseDouble(weightField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(titleFrame, "Please enter a valid weight.");
                return;
            }
            activityLevel = (String) activityLevelBox.getSelectedItem();
            titleFrame.dispose();
            createMainFrame();
        });

        titleFrame.add(titleLabel, BorderLayout.NORTH);
        titleFrame.add(inputPanel, BorderLayout.CENTER);
        titleFrame.add(startButton, BorderLayout.SOUTH);
        titleFrame.setLocationRelativeTo(null);
        titleFrame.setVisible(true);
    }

    private void createMainFrame() {
        frame = new JFrame("Food Nutrition Calculator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600);
        frame.setLayout(new BorderLayout(5, 5));

        // Top panel with total nutrition and recommendations
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        totalLabel = new JLabel("Total: Protein: 0g, Carbs: 0g, Fat: 0g, Calories: 0", SwingConstants.CENTER);
        recommendationLabel = new JLabel(getMacroRecommendations(), SwingConstants.CENTER);
        topPanel.add(totalLabel);
        topPanel.add(recommendationLabel);

        // Center panel: left for food list, right for added foods area
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        foodListModel = new DefaultListModel<>();
        foodList = new JList<>(foodListModel);
        JScrollPane foodListScrollPane = new JScrollPane(foodList);
        centerPanel.add(foodListScrollPane);

        addedFoodsArea = new JTextArea();
        addedFoodsArea.setEditable(false);
        JScrollPane addedFoodsScrollPane = new JScrollPane(addedFoodsArea);
        centerPanel.add(addedFoodsScrollPane);

        // Bottom panel for buttons
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Selected Food");
        JButton deleteButton = new JButton("Remove Selected Food");
        JButton exerciseButton = new JButton("View Exercises");

        addButton.addActionListener(e -> calculateTotal());
        deleteButton.addActionListener(e -> removeFoodNutrition());
        exerciseButton.addActionListener(e -> SwingUtilities.invokeLater(ExerciseSplitFrame::new));

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(exerciseButton);

        // Load food data from file and initialize maps
        foodData = new HashMap<>();
        foodQuantities = new HashMap<>();
        loadFoodData();

        // Add panels to frame
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void loadFoodData() {
        try (BufferedReader reader = new BufferedReader(new FileReader("foods.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    String name = parts[0].trim();
                    double protein = Double.parseDouble(parts[1].trim());
                    double carbs = Double.parseDouble(parts[2].trim());
                    double fat = Double.parseDouble(parts[3].trim());
                    double calories = Double.parseDouble(parts[4].trim());
                    foodData.put(name, new double[]{protein, carbs, fat, calories});
                    foodListModel.addElement(name);
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error reading foods.txt", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void calculateTotal() {
        for (String food : foodList.getSelectedValuesList()) {
            double[] values = foodData.get(food);
            totalProtein += values[0];
            totalCarbs += values[1];
            totalFat += values[2];
            totalCalories += values[3];
            foodQuantities.put(food, foodQuantities.getOrDefault(food, 0) + 1);
        }
        updateTotalLabel();
        updateAddedFoodsArea();
    }

    private void removeFoodNutrition() {
        for (String food : foodList.getSelectedValuesList()) {
            if (foodQuantities.containsKey(food) && foodQuantities.get(food) > 0) {
                double[] values = foodData.get(food);
                totalProtein -= values[0];
                totalCarbs -= values[1];
                totalFat -= values[2];
                totalCalories -= values[3];
                foodQuantities.put(food, foodQuantities.get(food) - 1);
            }
        }
        updateTotalLabel();
        updateAddedFoodsArea();
    }

    private void updateTotalLabel() {
        totalLabel.setText(String.format("Total: Protein: %.1fg, Carbs: %.1fg, Fat: %.1fg, Calories: %.0f",
                totalProtein, totalCarbs, totalFat, totalCalories));
    }

    private void updateAddedFoodsArea() {
        StringBuilder sb = new StringBuilder("Added Foods:\n");
        for (Map.Entry<String, Integer> entry : foodQuantities.entrySet()) {
            if (entry.getValue() > 0) {
                sb.append(entry.getKey()).append(" x ").append(entry.getValue()).append("\n");
            }
        }
        addedFoodsArea.setText(sb.toString());
    }

    private String getMacroRecommendations() {
        double calorieFactor = switch (activityLevel) {
            case "Sedentary" -> 25;
            case "Light" -> 28;
            case "Moderate" -> 30;
            case "Active" -> 33;
            default -> 36;
        };
        double calories = userWeight * calorieFactor;
        return String.format("Recommended Intake: %.0f kcal (Protein: %.1fg, Carbs: %.1fg, Fat: %.1fg)",
                calories, userWeight * 1.8, userWeight * 4, userWeight * 1);
    }

    // Separate window for exercise selection
    private static class ExerciseSplitFrame extends JFrame {
        private DefaultListModel<String> exerciseListModel;
        private JList<String> exerciseList;
        private JTextArea exerciseDetailsArea;
        private JButton selectButton, closeButton;

        public ExerciseSplitFrame() {
            setTitle("Exercise Selection");
            setSize(400, 300);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout(5, 5));
            setLocationRelativeTo(null);

            exerciseListModel = new DefaultListModel<>();
            exerciseListModel.addElement("Running");
            exerciseListModel.addElement("Cycling");
            exerciseListModel.addElement("Swimming");
            exerciseListModel.addElement("Weight Lifting");
            exerciseListModel.addElement("Yoga");

            exerciseList = new JList<>(exerciseListModel);
            JScrollPane listScrollPane = new JScrollPane(exerciseList);
            add(listScrollPane, BorderLayout.WEST);

            exerciseDetailsArea = new JTextArea();
            exerciseDetailsArea.setEditable(false);
            add(new JScrollPane(exerciseDetailsArea), BorderLayout.CENTER);

            selectButton = new JButton("Select Exercise");
            closeButton = new JButton("Close");

            selectButton.addActionListener(e -> {
                String selected = exerciseList.getSelectedValue();
                if (selected != null) {
                    switch (selected) {
                        case "Running":
                            exerciseDetailsArea.setText("Running: Excellent for cardiovascular endurance and calorie burning.");
                            break;
                        case "Cycling":
                            exerciseDetailsArea.setText("Cycling: Low-impact exercise that strengthens legs and improves stamina.");
                            break;
                        case "Swimming":
                            exerciseDetailsArea.setText("Swimming: Full-body workout offering benefits for strength and flexibility.");
                            break;
                        case "Weight Lifting":
                            exerciseDetailsArea.setText("Weight Lifting: Builds muscle, increases metabolism, and improves strength.");
                            break;
                        case "Yoga":
                            exerciseDetailsArea.setText("Yoga: Enhances flexibility, balance, and mental focus.");
                            break;
                        default:
                            exerciseDetailsArea.setText("Select an exercise to view details.");
                    }
                } else {
                    JOptionPane.showMessageDialog(ExerciseSplitFrame.this, "Please select an exercise.");
                }
            });
            closeButton.addActionListener(e -> dispose());

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(selectButton);
            buttonPanel.add(closeButton);
            add(buttonPanel, BorderLayout.SOUTH);

            setVisible(true);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FoodNutritionGUI::new);
    }
}

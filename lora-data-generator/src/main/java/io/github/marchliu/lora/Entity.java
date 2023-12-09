package io.github.marchliu.lora;

public class Entity {
    private String instruction;
    private String input;
    private String output;

    public Entity(String instruction, String input, String output) {
        this.instruction = instruction;
        this.input = input;
        this.output = output;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}

package net.defade.amestify.graphics;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL46;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

public class Shader {
    private final String vertexShader;
    private final String fragmentShader;

    private int shaderProgramId;
    private boolean beingUsed = false;

    public Shader(String shaderName) throws FileNotFoundException {
        InputStream shaderInputStream = Shader.class.getClassLoader().getResourceAsStream("shaders/" + shaderName);
        if (shaderInputStream == null) {
            throw new FileNotFoundException("Shader " + shaderName + " not found.");
        }

        BufferedReader stringReader = new BufferedReader(new InputStreamReader(shaderInputStream));
        StringBuilder vertexShaderBuilder = new StringBuilder();
        StringBuilder fragmentShaderBuilder = new StringBuilder();

        boolean isVertexShader = false;

        try {
            while (stringReader.ready()) {
                String line = stringReader.readLine();
                if (line.equals("#type vertex")) {
                    isVertexShader = true;
                    continue;
                }

                if (line.equals("#type fragment")) {
                    isVertexShader = false;
                    continue;
                }

                if (isVertexShader) {
                    vertexShaderBuilder.append(line).append("\n");
                } else {
                    fragmentShaderBuilder.append(line).append("\n");
                }
            }

            shaderInputStream.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        this.vertexShader = vertexShaderBuilder.toString();
        this.fragmentShader = fragmentShaderBuilder.toString();
    }

    public void init() {
        int vertexShaderId = GL46.glCreateShader(GL46.GL_VERTEX_SHADER);
        GL46.glShaderSource(vertexShaderId, vertexShader);
        GL46.glCompileShader(vertexShaderId);

        int vertexShaderCompilationSuccess = GL46.glGetShaderi(vertexShaderId, GL46.GL_COMPILE_STATUS);
        if(vertexShaderCompilationSuccess == GL46.GL_FALSE) {
            int length = GL46.glGetShaderi(vertexShaderId, GL46.GL_INFO_LOG_LENGTH);
            System.err.println("Couldn't compile the vertex shader: '" + GL46.glGetShaderInfoLog(vertexShaderId, length) + "'");
        }

        int fragmentShaderId = GL46.glCreateShader(GL46.GL_FRAGMENT_SHADER);
        GL46.glShaderSource(fragmentShaderId, fragmentShader);
        GL46.glCompileShader(fragmentShaderId);

        int fragmentShaderCompilationSuccess = GL46.glGetShaderi(fragmentShaderId, GL46.GL_COMPILE_STATUS);
        if(fragmentShaderCompilationSuccess == GL46.GL_FALSE) {
            int length = GL46.glGetShaderi(fragmentShaderId, GL46.GL_INFO_LOG_LENGTH);
            System.err.println("Couldn't compile the fragment shader: '" + GL46.glGetShaderInfoLog(fragmentShaderId, length) + "'");
        }

        shaderProgramId = GL46.glCreateProgram();
        GL46.glAttachShader(shaderProgramId, vertexShaderId);
        GL46.glAttachShader(shaderProgramId, fragmentShaderId);
        GL46.glLinkProgram(shaderProgramId);

        int linkingSuccessful = GL46.glGetProgrami(shaderProgramId, GL46.GL_LINK_STATUS);
        if(linkingSuccessful == GL46.GL_FALSE) {
            int length = GL46.glGetProgrami(shaderProgramId, GL46.GL_INFO_LOG_LENGTH);
            System.err.println("Couldn't link the shader: '" + GL46.glGetProgramInfoLog(shaderProgramId, length) + "'");
        }
    }

    public void attach() {
        if(!beingUsed) {
            GL46.glUseProgram(shaderProgramId);
            beingUsed = true;
        }
    }

    public void detach() {
        if(beingUsed) {
            GL46.glUseProgram(0);
            beingUsed = false;
        }
    }

    public void uploadMat4f(String varName, Matrix4f mat4) {
        int varLocation = GL46.glGetUniformLocation(shaderProgramId, varName);
        attach();
        FloatBuffer matBuffer = BufferUtils.createFloatBuffer(16);
        mat4.get(matBuffer);
        GL46.glUniformMatrix4fv(varLocation, false, matBuffer);
    }
}

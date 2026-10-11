package com.mycompany.coloredcube;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import org.joml.Matrix4f;
import java.io.InputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import com.jogamp.common.nio.Buffers;

public class ColoredCube implements GLEventListener, KeyListener {

    private FPSAnimator animator;
    private int shaderProgram;
    private int vao;
    private int vbo;
    private int mvpLocation;
    
    // Camera position in 3D space
private float cameraX = 0.0f;
private float cameraY = 0.0f;
private float cameraZ = 6.0f;

// Camera rotation
private float yaw = -90.0f;
private float pitch = 0.0f;

// Camera movement and rotation speeds
private float moveSpeed = 0.2f;
private float lookSpeed = 3.0f;
    
    private int vertexCount = 36;
    
    private final float[] vertices = {
    // Front face
    -1,-1, 1,  1,0,0,
     1,-1, 1,  0,1,0,
     1, 1, 1,  0,0,1,
    -1,-1, 1,  1,0,0,
     1, 1, 1,  0,0,1,
    -1, 1, 1,  1,1,0,

    // Back face
     1,-1,-1,  1,0,1,
    -1,-1,-1,  0,1,1,
    -1, 1,-1,  1,1,0,
     1,-1,-1,  1,0,1,
    -1, 1,-1,  1,1,0,
     1, 1,-1,  0,1,0,

    // Left face
    -1,-1,-1,  0,0,1,
    -1,-1, 1,  1,0,0,
    -1, 1, 1,  0,1,0,
    -1,-1,-1,  0,0,1,
    -1, 1, 1,  0,1,0,
    -1, 1,-1,  1,1,0,

    // Right face
     1,-1, 1,  1,1,0,
     1,-1,-1,  0,1,1,
     1, 1,-1,  1,0,1,
     1,-1, 1,  1,1,0,
     1, 1,-1,  1,0,1,
     1, 1, 1,  0,1,0,

    // Top face
    -1, 1, 1,  1,0,0,
     1, 1, 1,  0,1,0,
     1, 1,-1,  0,0,1,
    -1, 1, 1,  1,0,0,
     1, 1,-1,  0,0,1,
    -1, 1,-1,  1,1,0,

    // Bottom face
    -1,-1,-1,  0,1,1,
     1,-1,-1,  1,0,1,
     1,-1, 1,  1,1,0,
    -1,-1,-1,  0,1,1,
     1,-1, 1,  1,1,0,
    -1,-1, 1,  0,1,0
};

    
    private long startTime = System.nanoTime();
    
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f model = new Matrix4f();
    private final Matrix4f mvp = new Matrix4f();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GLProfile profile = GLProfile.get(GLProfile.GL3);
            GLCapabilities capabilities =
                    new GLCapabilities(profile);

            GLCanvas canvas = new GLCanvas(capabilities);

            ColoredCube cube = new ColoredCube();
            canvas.addGLEventListener(cube);
            
            canvas.addKeyListener(cube);
            canvas.setFocusable(true);

            JFrame frame = new JFrame("3D Colored Cube");
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);
            frame.add(canvas);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            
            canvas.requestFocusInWindow();

            cube.animator = new FPSAnimator(canvas, 60);
            cube.animator.start();
        });
    }

   @Override
public void init(GLAutoDrawable drawable) {
    GL3 gl = drawable.getGL().getGL3();

    // Enable depth testing
    gl.glEnable(GL.GL_DEPTH_TEST);

    // Set background color
    gl.glClearColor(0.05f, 0.05f, 0.10f, 1.0f);
    
        // Load, compile, and link the GLSL shaders
    createShaderProgram(gl);

    // Create vertex array object
    int[] vaos = new int[1];
    gl.glGenVertexArrays(1, vaos, 0);
    vao = vaos[0];

    gl.glBindVertexArray(vao);

    // Create vertex buffer object
    int[] vbos = new int[1];
    gl.glGenBuffers(1, vbos, 0);
    vbo = vbos[0];

    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo);

    // Upload vertex data to GPU
    FloatBuffer vertexBuffer =
            Buffers.newDirectFloatBuffer(vertices);

    gl.glBufferData(
            GL.GL_ARRAY_BUFFER,
            vertices.length * Float.BYTES,
            vertexBuffer,
            GL.GL_STATIC_DRAW
    );

    // Position attribute: location 0
    gl.glVertexAttribPointer(
            0, 3, GL.GL_FLOAT, false,
            6 * Float.BYTES, 0L
    );

    gl.glEnableVertexAttribArray(0);

    // Color attribute: location 1
    gl.glVertexAttribPointer(
            1, 3, GL.GL_FLOAT, false,
            6 * Float.BYTES, 3L * Float.BYTES
    );

    gl.glEnableVertexAttribArray(1);

    // Unbind VBO and VAO
    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    gl.glBindVertexArray(0);
}

   @Override
public void display(GLAutoDrawable drawable) {

    GL3 gl = drawable.getGL().getGL3();

    // Clear the screen and depth buffer
    gl.glClear(GL.GL_COLOR_BUFFER_BIT
            | GL.GL_DEPTH_BUFFER_BIT);

    // Calculate elapsed time in seconds
    float elapsedTime =
            (System.nanoTime() - startTime)
            / 1_000_000_000.0f;

    // Calculate the rotation angle
    float angle = elapsedTime
*	(float) Math.toRadians(45.0);

    // Rotate the cube around X and Y axes
    model.identity()
            .rotateY(angle)
            .rotateX(angle * 0.6f);
    
    // Update the camera view matrix
view.identity().lookAt(
    cameraX, cameraY, cameraZ,
    cameraX + (float) Math.cos(Math.toRadians(yaw))
*	(float) Math.cos(Math.toRadians(pitch)),
    cameraY + (float) Math.sin(Math.toRadians(pitch)),
    cameraZ + (float) Math.sin(Math.toRadians(yaw))
        * (float) Math.cos(Math.toRadians(pitch)),
    0.0f, 1.0f, 0.0f
);

    // Combine projection, view, and model matrices
    projection.mul(view, mvp);
    mvp.mul(model);

    // Activate the shader program
    gl.glUseProgram(shaderProgram);

    // Upload the MVP matrix to the vertex shader
    FloatBuffer matrixBuffer =
            Buffers.newDirectFloatBuffer(16);

    mvp.get(matrixBuffer);

    gl.glUniformMatrix4fv(
            mvpLocation, 1, false, matrixBuffer);

    // Bind the vertex array
    gl.glBindVertexArray(vao);

    // Draw the cube using 36 vertices
    gl.glDrawArrays(
            GL.GL_TRIANGLES, 0, vertexCount);

    // Unbind the vertex array
    gl.glBindVertexArray(0);

    // Deactivate the shader program
    gl.glUseProgram(0);
}


    @Override
public void reshape(GLAutoDrawable drawable,
                    int x, int y,
                    int width, int height) {

    GL3 gl = drawable.getGL().getGL3();

    // Prevent division by zero
    height = Math.max(height, 1);
    width = Math.max(width, 1);

    // Set the OpenGL viewport
    gl.glViewport(0, 0, width, height);

    // Calculate the aspect ratio
    float aspect = (float) width / height;

    // Configure perspective projection
    projection.identity().perspective(
            (float) Math.toRadians(45.0),
            aspect,
            0.1f,
            100.0f
    );

    // Move the viewing position away from the cube
    view.identity().lookAt(
    cameraX, cameraY, cameraZ,
    cameraX + (float) Math.cos(Math.toRadians(yaw))
*	(float) Math.cos(Math.toRadians(pitch)),
    cameraY + (float) Math.sin(Math.toRadians(pitch)),
    cameraZ + (float) Math.sin(Math.toRadians(yaw))
        * (float) Math.cos(Math.toRadians(pitch)),
    0.0f, 1.0f, 0.0f
);
}
    @Override
    public void dispose(GLAutoDrawable drawable) {
        // Graphics resources will be released here.
    }
    
    private String loadShaderSource(String fileName) {
    try (InputStream input = getClass()
            .getResourceAsStream("/" + fileName)) {

        if (input == null) {
            throw new RuntimeException(
                    "Shader file not found: " + fileName);
        }

        byte[] bytes = input.readAllBytes();

        return new String(
                bytes, StandardCharsets.UTF_8);

    } catch (IOException e) {
        throw new RuntimeException(
                "Unable to load shader: " + fileName, e);
    }
}

private int compileShader(
        GL3 gl, int shaderType, String source) {

    int shader = gl.glCreateShader(shaderType);

    String[] sources = {source};
    int[] lengths = {source.length()};

    gl.glShaderSource(
            shader, 1, sources, lengths, 0);

    gl.glCompileShader(shader);

    int[] status = new int[1];
    gl.glGetShaderiv(
            shader, GL3.GL_COMPILE_STATUS, status, 0);

    if (status[0] == GL.GL_FALSE) {
        int[] logLength = new int[1];

        gl.glGetShaderiv(
                shader, GL3.GL_INFO_LOG_LENGTH,
                logLength, 0);

        byte[] log = new byte[Math.max(1, logLength[0])];
        int[] actualLength = new int[1];

        gl.glGetShaderInfoLog(
                shader, log.length,
                actualLength, 0, log, 0);

        throw new RuntimeException(
                "Shader compilation failed: "
                + new String(log, 0, actualLength[0],
                        StandardCharsets.UTF_8));
    }

    return shader;
}

private void createShaderProgram(GL3 gl) {

    String vertexSource =
            loadShaderSource("vertex.glsl");

    String fragmentSource =
            loadShaderSource("fragment.glsl");

    int vertexShader = compileShader(
            gl, GL3.GL_VERTEX_SHADER, vertexSource);

    int fragmentShader = compileShader(
            gl, GL3.GL_FRAGMENT_SHADER, fragmentSource);

    shaderProgram = gl.glCreateProgram();

    gl.glAttachShader(shaderProgram, vertexShader);
    gl.glAttachShader(shaderProgram, fragmentShader);

    gl.glLinkProgram(shaderProgram);

    int[] status = new int[1];
    gl.glGetProgramiv(
            shaderProgram, GL3.GL_LINK_STATUS, status, 0);

    if (status[0] == GL.GL_FALSE) {
        int[] logLength = new int[1];
        gl.glGetProgramiv(
                shaderProgram, GL3.GL_INFO_LOG_LENGTH,
                logLength, 0);

        byte[] log = new byte[Math.max(1, logLength[0])];
        int[] actualLength = new int[1];

        gl.glGetProgramInfoLog(
                shaderProgram, log.length,
                actualLength, 0, log, 0);

        throw new RuntimeException(
                "Shader linking failed: "
                + new String(log, 0, actualLength[0],
                        StandardCharsets.UTF_8));
    }

    gl.glDeleteShader(vertexShader);
    gl.glDeleteShader(fragmentShader);

    mvpLocation = gl.glGetUniformLocation(
            shaderProgram, "mvpMatrix");
}
@Override
public void keyPressed(KeyEvent e) {
    float yawRadians = (float) Math.toRadians(yaw);

    switch (e.getKeyCode()) {
        case KeyEvent.VK_W:
            cameraX += (float) Math.cos(yawRadians) * moveSpeed;
            cameraZ += (float) Math.sin(yawRadians) * moveSpeed;
            break;

        case KeyEvent.VK_S:
            cameraX -= (float) Math.cos(yawRadians) * moveSpeed;
            cameraZ -= (float) Math.sin(yawRadians) * moveSpeed;
            break;

        case KeyEvent.VK_A:
            cameraX += (float) Math.sin(yawRadians) * moveSpeed;
            cameraZ -= (float) Math.cos(yawRadians) * moveSpeed;
            break;

        case KeyEvent.VK_D:
            cameraX -= (float) Math.sin(yawRadians) * moveSpeed;
            cameraZ += (float) Math.cos(yawRadians) * moveSpeed;
            break;

        case KeyEvent.VK_Q:
            cameraY -= moveSpeed;
            break;

        case KeyEvent.VK_E:
            cameraY += moveSpeed;
            break;

        case KeyEvent.VK_LEFT:
            yaw -= lookSpeed;
            break;

        case KeyEvent.VK_RIGHT:
            yaw += lookSpeed;
            break;

        case KeyEvent.VK_UP:
            pitch = Math.min(89.0f, pitch + lookSpeed);
            break;

        case KeyEvent.VK_DOWN:
            pitch = Math.max(-89.0f, pitch - lookSpeed);
            break;

        default:
            break;
    }
}

@Override
public void keyReleased(KeyEvent e) {
    // No action required when a key is released.
}

@Override
public void keyTyped(KeyEvent e) {
    // Keyboard controls are handled by keyPressed().
}

}

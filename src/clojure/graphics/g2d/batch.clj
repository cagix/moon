(ns clojure.graphics.g2d.batch
  "
/** A Batch is used to draw 2D rectangles that reference a texture (region). The class will batch the drawing commands and
 * optimize them for processing by the GPU.
 * <p>
 * To draw something with a Batch one has to first call the {@link Batch#begin()} method which will setup appropriate render
 * states. When you are done with drawing you have to call {@link Batch#end()} which will actually draw the things you specified.
 * <p>
 * All drawing commands of the Batch operate in screen coordinates. The screen coordinate system has an x-axis pointing to the
 * right, an y-axis pointing upwards and the origin is in the lower left corner of the screen. You can also provide your own
 * transformation and projection matrices if you so wish.
 * <p>
 * A Batch is managed. In case the OpenGL context is lost all OpenGL resources a Batch uses internally get invalidated. A context
 * is lost when a user switches to another application or receives an incoming call on Android. A Batch will be automatically
 * reloaded after the OpenGL context is restored.
 * <p>
 * A Batch is a pretty heavy object so you should only ever have one in your program.
 * <p>
 * A Batch works with OpenGL ES 2.0. It will use its own custom shader to draw all provided sprites. You can set your own custom
 * shader via {@link #setShader(ShaderProgram)}.
 * <p>
 * A Batch has to be disposed if it is no longer used.
  "
  )

(defprotocol Batch
  (draw! [_ texture-region x y [w h] rotation]
         "TODO "
         )
  (set-color! [_ [r g b a]]
              "Sets the color used to tint images when they are added to the Batch. Default is {@link Color#WHITE}.")
  (set-projection-matrix! [_ matrix]
                          "
	/** Sets the projection matrix to be used by this Batch. If this is called inside a {@link #begin()}/{@link #end()} block, the
	 * current batch is flushed to the gpu. */
                          "
                          )
  (begin! [_]
          "
	/** Sets up the Batch for drawing. This will disable depth buffer writing. It enables blending and texturing. If you have more
	 * texture units enabled than the first one you have to disable them before calling this. Uses a screen coordinate system by
	 * default where everything is given in pixels. You can specify your own projection and modelview matrices via
	 * {@link #setProjectionMatrix(Matrix4)} and {@link #setTransformMatrix(Matrix4)}. */
          "

          )
  (end! [_]
        "
	/** Finishes off rendering. Enables depth writes, disables blending and texturing. Must always be called after a call to
	 * {@link #begin()} */
        "

        ))

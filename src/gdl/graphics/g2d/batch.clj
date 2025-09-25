(ns gdl.graphics.g2d.batch
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
  (:import (com.badlogic.gdx.graphics.g2d Batch)))

(defn draw! [^Batch batch texture-region x y [w h] rotation]
  (.draw batch
         texture-region
         x
         y
         (/ (float w) 2) ; origin-x
         (/ (float h) 2) ; origin-y
         w
         h
         1 ; scale-x
         1 ; scale-y
         rotation))

(defn set-color! [^Batch batch [r g b a]]
  (.setColor batch r g b a))

(defn set-projection-matrix! [^Batch batch matrix]
  (.setProjectionMatrix batch matrix))

(defn begin! [^Batch batch]
  (.begin batch))

(defn end! [^Batch batch]
  (.end batch))

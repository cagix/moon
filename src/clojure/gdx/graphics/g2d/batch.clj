(ns clojure.gdx.graphics.g2d.batch
  "A Batch is used to draw 2D rectangles that reference a texture (region). The class will batch the drawing commands and optimize them for processing by the GPU.

  To draw something with a Batch one has to first call the begin() method which will setup appropriate render states. When you are done with drawing you have to call end() which will actually draw the things you specified.

  All drawing commands of the Batch operate in screen coordinates. The screen coordinate system has an x-axis pointing to the right, an y-axis pointing upwards and the origin is in the lower left corner of the screen. You can also provide your own transformation and projection matrices if you so wish.

  A Batch is managed. In case the OpenGL context is lost all OpenGL resources a Batch uses internally get invalidated. A context is lost when a user switches to another application or receives an incoming call on Android. A Batch will be automatically reloaded after the OpenGL context is restored.

  A Batch is a pretty heavy object so you should only ever have one in your program.

  A Batch works with OpenGL ES 2.0. It will use its own custom shader to draw all provided sprites. You can set your own custom shader via setShader(ShaderProgram).

  A Batch has to be disposed if it is no longer used."
  (:import (com.badlogic.gdx.graphics.g2d Batch)))

(defn set-projection-matrix [this projection]
  (Batch/.setProjectionMatrix this projection))

(defn begin [this]
  (Batch/.begin this))

(defn end [this]
  (Batch/.end this))

(defn set-color [this color]
  (Batch/.setColor this color))

(defn draw [this texture-region {:keys [x y origin-x origin-y width height scale-x scale-y rotation]}]
  (Batch/.draw this
               texture-region
               x
               y
               origin-x
               origin-y
               width
               height
               scale-x
               scale-y
               rotation))

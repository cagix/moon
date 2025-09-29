(ns gdl.application
  (:require [com.badlogic.gdx.backends.lwjgl :as lwjgl])
  (:import (com.badlogic.gdx ApplicationListener
                             Gdx)))

(defn start! [config]
  (lwjgl/application
   (reify ApplicationListener
     (create [_]
       ((:create config) {:ctx/audio    Gdx/audio
                          :ctx/files    Gdx/files
                          :ctx/graphics Gdx/graphics
                          :ctx/input    Gdx/input}))
     (dispose [_]
       ((:dispose config)))
     (render [_]
       ((:render config)))
     (resize [_ width height]
       ((:resize config) width height))
     (pause [_])
     (resume [_]))
   config))

(defn post-runnable! [f]
  (.postRunnable Gdx/app f))

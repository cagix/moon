(ns gdl.platform.libgdx
  (:require [clojure.graphics.2d.batch]
            [gdl.audio]
            [gdl.files.file-handle]
            [gdl.utils]))

(extend-type com.badlogic.gdx.files.FileHandle
  gdl.files.file-handle/FileHandle
  (list [this]
    (.list this))
  (directory? [this]
    (.isDirectory this))
  (extension [this]
    (.extension this))
  (path [this]
    (.path this)))

(extend-type com.badlogic.gdx.audio.Sound
  gdl.audio/Sound
  (play [this]
    (.play this)))

(extend-type com.badlogic.gdx.utils.Disposable
  gdl.utils/Disposable
  (dispose [this]
    (.dispose this)))

(extend-type com.badlogic.gdx.graphics.g2d.Batch
  clojure.graphics.2d.batch/Batch
  (set-projection-matrix [this projection]
    (.setProjectionMatrix this projection))
  (begin [this]
    (.begin this))
  (end
    [this]
    (.end this))
  (set-color [this color]
    (.setColor this color))
  (draw [this texture-region {:keys [x y origin-x origin-y width height scale-x scale-y rotation]}]
    (.draw this
           texture-region
           x
           y
           origin-x
           origin-y
           width
           height
           scale-x
           scale-y
           rotation)))

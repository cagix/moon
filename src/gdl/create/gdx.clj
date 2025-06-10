(ns gdl.create.gdx
  (:require [clojure.gdx.interop :as interop]
            [gdl.app]
            [gdl.audio]
            [gdl.file]
            [gdl.fs]
            [gdl.graphics]
            [gdl.input]
            [gdl.utils.disposable])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Pixmap)
           (com.badlogic.gdx.utils Disposable)))

(extend-type Sound
  gdl.audio/Sound
  (play! [this]
    (.play this)))

(extend-type Disposable
  gdl.utils.disposable/Disposable
  (dispose! [object]
    (.dispose object)))

(extend-type FileHandle
  gdl.file/File
  (list [this]
    (.list this))
  (directory? [this]
    (.isDirectory this))
  (extension [this]
    (.extension this))
  (path [this]
    (.path this)))

(defrecord Context [app
                    audio
                    files
                    graphics
                    input]
  gdl.app/Application
  (post-runnable! [_ runnable]
    (.postRunnable app runnable))

  gdl.fs/FileSystem
  (internal [_ path]
    (.internal files path))

  gdl.audio/Sounds
  (sound [_ path]
    (.newSound audio (.internal files path)))

  gdl.graphics/Cursors
  (cursor [_ path [hotspot-x hotspot-y]]
    (let [pixmap (Pixmap. (.internal files path))
          cursor (.newCursor graphics pixmap hotspot-x hotspot-y)]
      (.dispose pixmap)
      cursor))

  gdl.input/Input
  (set-input-processor! [_ input-processor]
    (.setInputProcessor input input-processor))

  (button-just-pressed? [_ button]
    (.isButtonJustPressed input (interop/k->input-button button)))

  (key-pressed? [_ key]
    (.isKeyPressed input (interop/k->input-key key)))

  (key-just-pressed? [_ key]
    (.isKeyJustPressed input (interop/k->input-key key)))

  (mouse-position [_]
    [(.getX input)
     (.getY input)]))

(defn do! [_ctx _params]
  (map->Context {:app      Gdx/app
                 :audio    Gdx/audio
                 :files    Gdx/files
                 :graphics Gdx/graphics
                 :input    Gdx/input}))

(ns cdq.game.create.get-gdx
  (:require [cdq.audio]
            [cdq.input]
            [cdq.impl.graphics])
  (:import (com.badlogic.gdx Gdx
                             Input)
           (com.badlogic.gdx.audio Sound)))

(defn- create-audio
  [sounds]
  (reify cdq.audio/Audio
    (sound-names [_]
      (map first sounds))

    (play! [_ sound-name]
      (assert (contains? sounds sound-name) (str sound-name))
      (Sound/.play (get sounds sound-name)))

    (dispose! [_]
      (run! Sound/.dispose (vals sounds)))))

(defn do! [ctx config]
  (let [audio    Gdx/audio
        files    Gdx/files
        graphics Gdx/graphics
        input    Gdx/input
        {:keys [sound-names path-format]} (:audio config)
        sound-name->file-handle (into {}
                                      (for [sound-name sound-names
                                            :let [path (format path-format sound-name)]]
                                        [sound-name
                                         (.internal files path)]))
        sounds (into {}
                     (for [[sound-name file-handle] sound-name->file-handle]
                       [sound-name
                        (.newSound audio file-handle)]))
        ]
    (assoc ctx
           :ctx/audio (create-audio sounds)
           :ctx/graphics (cdq.impl.graphics/create! graphics files (:graphics config))
           :ctx/input input)))

(extend-type Input
  cdq.input/Input
  (set-processor! [this input-processor]
    (.setInputProcessor this input-processor))

  (key-pressed? [this key]
    (.isKeyPressed this key))

  (key-just-pressed? [this key]
    (.isKeyJustPressed this key))

  (button-just-pressed? [this button]
    (.isButtonJustPressed this button))

  (mouse-position [this]
    [(.getX this)
     (.getY this)]))

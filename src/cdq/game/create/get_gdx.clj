(ns cdq.game.create.get-gdx
  (:require [cdq.audio]
            [cdq.input]
            [cdq.impl.graphics])
  (:import (com.badlogic.gdx Gdx
                             Input)
           (com.badlogic.gdx.audio Sound)))

(defn- load-sound [path]
  (.newSound Gdx/audio (.internal Gdx/files path)))

(defn- create-audio
  [{:keys [sound-names path-format]}]
  (let [sounds (into {}
                     (for [sound-name sound-names]
                       [sound-name
                        (->> sound-name
                             (format path-format)
                             load-sound)]))]
    (reify cdq.audio/Audio
      (sound-names [_]
        (map first sounds))

      (play! [_ sound-name]
        (assert (contains? sounds sound-name) (str sound-name))
        (Sound/.play (get sounds sound-name)))

      (dispose! [_]
        (run! Sound/.dispose (vals sounds))))))

(defn do! [ctx config]
  (assoc ctx
         :ctx/audio (create-audio (:audio config))
         :ctx/graphics (cdq.impl.graphics/create! (:graphics config))
         :ctx/input Gdx/input))

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

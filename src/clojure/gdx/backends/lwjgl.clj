(ns clojure.gdx.backends.lwjgl
  (:require [clojure.gdx.audio]
            [clojure.gdx.input]
            [com.badlogic.gdx :as gdx]
            [com.badlogic.gdx.audio :as audio]
            [com.badlogic.gdx.files :as files]
            [com.badlogic.gdx.input :as input]
            [com.badlogic.gdx.input.buttons :as input-buttons]
            [com.badlogic.gdx.input.keys    :as input-keys]
            [com.badlogic.gdx.application.listener :as listener]
            [com.badlogic.gdx.backends.lwjgl3.application :as application]
            [com.badlogic.gdx.backends.lwjgl3.application.config :as config]))

(defrecord Context []
  clojure.gdx.audio/Audio
  (sound [{:keys [clojure.gdx/audio
                  clojure.gdx/files]}
          path]
    (audio/sound audio (files/internal files path))))

(defn create [{:keys [create] :as listener} config]
  (application/create (listener/create
                       (assoc listener :create
                              (fn []
                                (create (map->Context
                                         (let [{:keys [audio files graphics input]} (gdx/context)]
                                           {:clojure.gdx/audio    audio
                                            :clojure.gdx/files    files
                                            :clojure.gdx/graphics graphics
                                            :clojure.gdx/input    input}))))))
                      (config/create config)))

(extend-type com.badlogic.gdx.Input
  clojure.gdx.input/Input
  (button-just-pressed? [this button]
    {:pre [(contains? input-buttons/k->value button)]}
    (input/button-just-pressed? this (input-buttons/k->value button)))

  (key-pressed? [this key]
    (assert (contains? input-keys/k->value key)
            (str "(pr-str key): "(pr-str key)))
    (input/key-pressed? this (input-keys/k->value key)))

  (key-just-pressed? [this key]
    {:pre [(contains? input-keys/k->value key)]}
    (input/key-just-pressed? this (input-keys/k->value key)))

  (set-processor! [this input-processor]
    (input/set-processor! this input-processor))

  (mouse-position [this]
    [(input/x this)
     (input/y this)]))

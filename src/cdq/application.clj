(ns cdq.application
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.input :as input]
            gdl.input)
  (:import (com.badlogic.gdx ApplicationAdapter
                             Gdx)))

(defn- make-input [input]
  (reify gdl.input/Input
    (button-just-pressed? [_ button]
      (clojure.gdx.input/button-just-pressed? input button))

    (key-pressed? [_ key]
      (clojure.gdx.input/key-pressed? input key))

    (key-just-pressed? [_ key]
      (clojure.gdx.input/key-just-pressed? input key))

    (set-processor! [_ input-processor]
      (.setInputProcessor input input-processor))))

(defn- get-context []
  {;:clojure.gdx/app      Gdx/app
   :clojure.gdx/files    Gdx/files
   :clojure.gdx/graphics Gdx/graphics
   :clojure.gdx/input    (make-input Gdx/input)})

(def state (atom nil))

(defn start! [{::keys [create!
                       dispose!
                       render!
                       resize!
                       validate-ctx-schema
                       lwjgl-app-config]
               :as config}]
  (lwjgl/application lwjgl-app-config
                     (proxy [ApplicationAdapter] []
                       (create []
                         (reset! state (create! (get-context) config))
                         (validate-ctx-schema @state))

                       (dispose []
                         (dispose! @state))

                       (render []
                         (validate-ctx-schema @state)
                         (swap! state render!)
                         (validate-ctx-schema @state))

                       (resize [width height]
                         (resize! @state width height)))))

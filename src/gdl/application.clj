(ns gdl.application
  (:require [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.input :as input]
            [gdl.files]
            [gdl.input])
  (:import (com.badlogic.gdx ApplicationAdapter
                             Files
                             Gdx)))

(defn- make-files [^Files files]
  (reify gdl.files/Files
    (internal [_ path]
      (.internal files path))))

(defn- make-input [input]
  (reify gdl.input/Input
    (button-just-pressed? [_ button]
      (input/button-just-pressed? input button))

    (key-pressed? [_ key]
      (input/key-pressed? input key))

    (key-just-pressed? [_ key]
      (input/key-just-pressed? input key))

    (set-processor! [_ input-processor]
      (input/set-processor! input input-processor))

    (mouse-position [_]
      (input/mouse-position input))))

(defn- create-context [config]
  {:ctx/input (make-input Gdx/input)
   :ctx/files (make-files Gdx/files)
   })

(def state (atom nil))

(defn start! [{::keys [create!
                       dispose!
                       render!
                       resize!
                       lwjgl-app-config]
               :as config}]
  (lwjgl/application lwjgl-app-config
                     (proxy [ApplicationAdapter] []
                       (create []
                         (reset! state (create! (create-context config)
                                                config)))

                       (dispose []
                         (dispose! @state))

                       (render []
                         (swap! state render!))

                       (resize [width height]
                         (resize! @state width height)))))

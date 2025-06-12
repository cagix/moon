(ns gdl.application
  (:require [clojure.gdx :as gdx]
            [gdl.app]
            [gdl.input]
            [gdl.utils.disposable]
            [gdx.backends.lwjgl.application.config :as application-config]
            [gdx.utils.shared-library-loader :as shared-library-loader])
  (:import (com.badlogic.gdx ApplicationAdapter
                             Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application)))

(defn- execute! [[f params]]
  ;(println "execute!" [f params])
  (f params))

; 0. reda config chere
; 1. pass Gdx state
; 2. txs start inside cereate/dispoes/render/resize only ?
; 3. state here ?
; 4. inside graphics again txs !?
(defn start!
  [os-config
   lwjgl3-config
   {:keys [create! dispose! render! resize!]}]
  (run! execute! (get os-config (shared-library-loader/operating-system)))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create []
                          (create! {:ctx/app   Gdx/app
                                    :ctx/input Gdx/input}))
                        (dispose []
                          (dispose!))
                        (render []
                          (render!))
                        (resize [width height]
                          (resize! width height)))
                      (application-config/create lwjgl3-config)))

(extend-type com.badlogic.gdx.Application
  gdl.app/Application
  (post-runnable! [this runnable]
    (.postRunnable this runnable)))

(extend-type com.badlogic.gdx.Input
  gdl.input/Input
  (button-just-pressed? [this button]
    (.isButtonJustPressed this (gdx/k->Input$Buttons button)))

  (key-pressed? [this key]
    (.isKeyPressed this (gdx/k->Input$Keys key)))

  (key-just-pressed? [this key]
    (.isKeyJustPressed this (gdx/k->Input$Keys key)))

  (mouse-position [this]
    [(.getX this)
     (.getY this)])

  (set-processor! [this input-processor]
    (.setInputProcessor this input-processor)))

(extend-type com.badlogic.gdx.utils.Disposable
  gdl.utils.disposable/Disposable
  (dispose! [object]
    (.dispose object)))

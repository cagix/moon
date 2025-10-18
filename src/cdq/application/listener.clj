(ns cdq.application.listener
  (:require [clojure.core-ext :refer [pipeline]]
            [clojure.gdx :as gdx]
            [clojure.gdx.application.listener :as listener]))

(def state (atom nil))

(import '(com.badlogic.gdx Gdx))

(defn- set-gdx-nil! []
  (set! Gdx/app nil)
  (set! Gdx/graphics nil)
  (set! Gdx/audio nil)
  (set! Gdx/input nil)
  (set! Gdx/files nil)
  (set! Gdx/net nil)
  (set! Gdx/gl nil)
  (set! Gdx/gl20 nil)
  (set! Gdx/gl30 nil)
  (set! Gdx/gl31 nil)
  (set! Gdx/gl32 nil))

; TODO make the change local
; just Lwjgl3ApplicationListener
; gets passed application

(defn create [config]
  (listener/create
   {:create (fn []
              ; pass application
              ; or 'ctx' is actually application

              (let [gdx (gdx/context)]
                #_(set-gdx-nil!)
                ; com.badlogic.gdx.graphics.glutils/VertexBufferObject
                ; Gdx.gl20.glGenBuffer();
                (reset! state ((:create config) gdx config))))

    :dispose (fn []
               ((:dispose config) @state))

    :render (fn []
              (swap! state pipeline (:render-pipeline config)))

    :resize (fn [width height]
              ((:resize config) @state width height))

    :pause (fn [])
    :resume (fn [])}))

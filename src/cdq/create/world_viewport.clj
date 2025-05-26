(ns cdq.create.world-viewport
  (:require [clojure.gdx.graphics.camera :as camera]
            [gdl.application]
            [gdl.c :as c]
            [gdl.viewport :as viewport]))

(def ^:private -k :ctx/world-viewport)

(defn add [ctx config]
  (assoc ctx -k (viewport/world-viewport (:ctx/world-unit-scale ctx)
                                         (:world-viewport config))))

(extend-type gdl.application.Context
  c/WorldViewport
  (set-camera-position! [ctx position]
    (camera/set-position! (:camera (-k ctx)) position))

  (world-mouse-position [ctx]
    (viewport/mouse-position (-k ctx)))

  (world-viewport-width [ctx]
    (:width (-k ctx)))

  (world-viewport-height [ctx]
    (:height (-k ctx)))

  (camera-position [ctx]
    (camera/position (:camera (-k ctx))))

  (inc-zoom! [ctx amount]
    (camera/inc-zoom! (:camera (-k ctx)) amount))

  (camera-frustum [ctx]
    (camera/frustum (:camera (-k ctx))))

  (visible-tiles [ctx]
    (camera/visible-tiles (:camera (-k ctx))))

  (camera-zoom [ctx]
    (camera/zoom (:camera (-k ctx)))))

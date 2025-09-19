(ns com.badlogic.gdx.utils.viewport.fit-viewport
  (:require [clojure.utils :refer [clamp]]
            [com.badlogic.gdx.math.vector2 :as vector2]
            [gdl.graphics.viewport])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.utils.viewport FitViewport)))

(defn create [width height camera]
  (proxy [FitViewport ILookup] [width height camera]
    (valAt [k]
      (let [^FitViewport this this]
        (case k
          :viewport/width             (.getWorldWidth      this)
          :viewport/height            (.getWorldHeight     this)
          :viewport/camera            (.getCamera          this)
          :viewport/left-gutter-width (.getLeftGutterWidth this)
          :viewport/right-gutter-x    (.getRightGutterX    this)
          :viewport/top-gutter-height (.getTopGutterHeight this)
          :viewport/top-gutter-y      (.getTopGutterY      this))))))

(defn- unproject [viewport x y]
  (-> viewport
      (.unproject (vector2/->java x y))
      vector2/->clj))

(extend-type FitViewport
  gdl.graphics.viewport/Viewport
  (update! [this width height {:keys [center?]}]
    (.update this width height center?))

  (unproject [this [x y]]
    (unproject this
               (clamp x
                      (:viewport/left-gutter-width this)
                      (:viewport/right-gutter-x    this))
               (clamp y
                      (:viewport/top-gutter-height this)
                      (:viewport/top-gutter-y      this)))))

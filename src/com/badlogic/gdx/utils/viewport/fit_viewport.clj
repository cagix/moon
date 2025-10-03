(ns com.badlogic.gdx.utils.viewport.fit-viewport
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

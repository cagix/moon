(ns cdq.graphics.camera
  (:require [clojure.gdx.graphics.orthographic-camera :as camera]
            [clojure.gdx.viewport :as viewport]))

(defn position [{:keys [graphics/world-viewport]}]
  (camera/position (viewport/camera world-viewport)))

(defn visible-tiles [{:keys [graphics/world-viewport]}]
  (camera/visible-tiles (viewport/camera world-viewport)))

(defn frustum [{:keys [graphics/world-viewport]}]
  (camera/frustum (viewport/camera world-viewport)))

(defn zoom [{:keys [graphics/world-viewport]}]
  (camera/zoom (viewport/camera world-viewport)))

(defn change-zoom! [{:keys [graphics/world-viewport]} amount]
  (camera/inc-zoom! (viewport/camera world-viewport) amount))

(defn set-position! [{:keys [graphics/world-viewport]} position]
  (camera/set-position! (viewport/camera world-viewport) position))

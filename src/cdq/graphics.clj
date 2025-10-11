(ns cdq.graphics
  (:require [clojure.gdx.graphics :as graphics]
            [clojure.gdx.orthographic-camera :as camera]
            [clojure.gdx.viewport :as viewport]
            [clojure.gdx.utils.disposable :as disposable]))

(defn clear-screen! [{:keys [graphics/core]} color]
  (graphics/clear! core color))

(defn set-cursor! [{:keys [graphics/core
                           graphics/cursors]}
                   cursor-key]
  (assert (contains? cursors cursor-key))
  (graphics/set-cursor! core (get cursors cursor-key)))

(defn frames-per-second [{:keys [graphics/core]}]
  (graphics/frames-per-second core))

(defn delta-time [{:keys [graphics/core]}]
  (graphics/delta-time core))

(defn dispose!
  [{:keys [graphics/batch
           graphics/cursors
           graphics/default-font
           graphics/shape-drawer-texture
           graphics/textures]}]
  (disposable/dispose! batch)
  (run! disposable/dispose! (vals cursors))
  (disposable/dispose! default-font)
  (disposable/dispose! shape-drawer-texture)
  (run! disposable/dispose! (vals textures)))

(defprotocol Graphics
  (draw! [_ draws]))

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

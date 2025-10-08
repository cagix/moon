(ns cdq.graphics
  (:require [clojure.gdx.graphics.orthographic-camera :as camera]
            [clojure.gdx.viewport :as viewport])
  (:import (com.badlogic.gdx.utils Disposable)))

(defn dispose!
  [{:keys [graphics/batch
           graphics/cursors
           graphics/default-font
           graphics/shape-drawer-texture
           graphics/textures]}]
  (Disposable/.dispose batch)
  (run! Disposable/.dispose (vals cursors))
  (Disposable/.dispose default-font)
  (Disposable/.dispose shape-drawer-texture)
  (run! Disposable/.dispose (vals textures)))

(defprotocol Graphics
  (clear! [_ [r g b a]])
  (set-cursor! [_ cursor-key])
  (delta-time [_])
  (frames-per-second [_])
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

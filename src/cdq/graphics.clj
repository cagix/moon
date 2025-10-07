(ns cdq.graphics
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
  (frames-per-second [_]))

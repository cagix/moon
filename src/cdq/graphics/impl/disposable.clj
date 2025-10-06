(ns cdq.graphics.impl.disposable
  (:require [clojure.disposable :as d]))

(defn dispose!
  [{:keys [graphics/batch
           graphics/cursors
           graphics/default-font
           graphics/shape-drawer-texture
           graphics/textures]}]
  (d/dispose! batch)
  (run! d/dispose! (vals cursors))
  (d/dispose! default-font)
  (d/dispose! shape-drawer-texture)
  (run! d/dispose! (vals textures)))

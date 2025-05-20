(ns cdq.application.create.shape-drawer
  (:require [gdl.graphics :as graphics]))

(defn do! [{:keys [ctx/batch
                   ctx/shape-drawer-texture]}]
  (graphics/shape-drawer batch
                         (graphics/texture-region shape-drawer-texture 1 0 1 1)))

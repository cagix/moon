(ns cdq.ctx.create.graphics
  (:require [cdq.graphics.impl :as graphics]))

(defn do!
  [{:keys [ctx/files
           ctx/graphics]
    :as ctx}
   params]
  (assoc ctx :ctx/graphics (graphics/create graphics files params)))

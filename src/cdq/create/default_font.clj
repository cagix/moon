(ns cdq.create.default-font
  (:require [gdl.graphics :as graphics]))

(defn do! [{:keys [ctx/config]}]
  (graphics/truetype-font (:default-font config)))

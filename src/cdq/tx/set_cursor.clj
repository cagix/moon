(ns cdq.tx.set-cursor
  (:require [cdq.utils :as utils]
            [gdl.graphics :as graphics]))

(defn do! [{:keys [ctx/cursors]} cursor]
  (graphics/set-cursor! (utils/safe-get cursors cursor)))

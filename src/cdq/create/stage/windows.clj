(ns cdq.create.stage.windows
  (:require [cdq.ui :as ui]
            [cdq.utils :as utils]))

(defn create [actors context]
  (ui/group {:id :windows
             :actors (map (fn [fn-invoc]
                            (utils/req-resolve-call fn-invoc context))
                          actors)}))

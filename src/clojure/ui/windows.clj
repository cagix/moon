(ns clojure.ui.windows
  (:require [clojure.ui :as ui]
            [clojure.utils :as utils]))

(defn create [actors context]
  (ui/group {:id :windows
             :actors (map (fn [fn-invoc]
                            (utils/req-resolve-call fn-invoc context))
                          actors)}))

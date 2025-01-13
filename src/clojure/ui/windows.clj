(ns clojure.ui.windows
  (:require [clojure.ui :as ui]
            [clojure.utils :as utils]))

(defn create [context actors]
  (ui/group {:id :windows
             :actors (map (fn [create] (create context)) actors)}))

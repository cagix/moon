(ns cdq.ui.separator
  (:require [clojure.vis-ui.separator :as separator]))

(defn horizontal [colspan]
  {:actor (separator/horizontal)
   :pad-top 2
   :pad-bottom 2
   :colspan colspan
   :fill-x? true
   :expand-x? true})

(defn vertical []
  {:actor (separator/vertical)
   :pad-top 2
   :pad-bottom 2
   :fill-y? true
   :expand-y? true})

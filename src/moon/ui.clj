(ns moon.ui
  (:require [gdl.ui :as ui]
            [moon.app :as app]))

(defc :moon.ui
  (app/create [[_ skin-scale]]
    (ui/load! skin-scale))
  (app/dispose [_]
    (ui/dispose!)))

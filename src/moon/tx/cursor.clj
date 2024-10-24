(ns moon.tx.cursor
  (:require [moon.component :as component :refer [defc]]
            [moon.graphics :as g]))

(defc :tx/cursor
  (component/handle [[_ cursor-key]]
    (g/set-cursor! cursor-key)
    nil))

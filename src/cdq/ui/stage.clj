(ns cdq.ui.stage
  (:import (cdq.ui Stage)))

(defn ctx [^Stage stage]
  (.ctx stage))

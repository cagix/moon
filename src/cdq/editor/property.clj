(ns cdq.editor.property)

(defn image [{:keys [entity/image entity/animation]}]
  (or image
      (first (:animation/frames animation))))

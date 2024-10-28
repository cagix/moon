(ns moon.graphics.text
  (:require [gdl.graphics.text :as text]
            [gdl.utils :refer [dispose]]
            [moon.app :as app]
            [moon.component :refer [defc]]
            [moon.graphics.batch :refer [batch]]
            [moon.graphics.view :as view]))

(declare ^:private default-font)

(defc :moon.graphics.text
  (app/create [[_ true-type-font]]
    (bind-root #'default-font (or (and true-type-font (text/truetype-font true-type-font))
                                  (text/default-font))))
  (app/dispose [_]
    (dispose default-font)))

(defn draw
  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
  [{:keys [x y text font h-align up? scale] :as opts}]
  (text/draw (or font default-font)
             view/*unit-scale*
             batch
             opts))

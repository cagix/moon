(ns cdq.graphics.impl.draws
  (:require [cdq.graphics.draw.arc]
            [cdq.graphics.draw.circle]
            [cdq.graphics.draw.ellipse]
            [cdq.graphics.draw.filled-circle]
            [cdq.graphics.draw.filled-ellipse]
            [cdq.graphics.draw.filled-rectangle]
            [cdq.graphics.draw.grid]
            [cdq.graphics.draw.line]
            [cdq.graphics.draw.rectangle]
            [cdq.graphics.draw.sector]
            [cdq.graphics.draw.text]
            [cdq.graphics.draw.texture-region]
            [cdq.graphics.draw.with-line-width]))

(def ^:private draw-fns
  {:draw/with-line-width  cdq.graphics.draw.with-line-width/do!
   :draw/grid             cdq.graphics.draw.grid/do!
   :draw/texture-region   cdq.graphics.draw.texture-region/do!
   :draw/text             cdq.graphics.draw.text/do!
   :draw/ellipse          cdq.graphics.draw.ellipse/do!
   :draw/filled-ellipse   cdq.graphics.draw.filled-ellipse/do!
   :draw/circle           cdq.graphics.draw.circle/do!
   :draw/filled-circle    cdq.graphics.draw.filled-circle/do!
   :draw/rectangle        cdq.graphics.draw.rectangle/do!
   :draw/filled-rectangle cdq.graphics.draw.filled-rectangle/do!
   :draw/arc              cdq.graphics.draw.arc/do!
   :draw/sector           cdq.graphics.draw.sector/do!
   :draw/line             cdq.graphics.draw.line/do!})

(defn handle! [graphics draws]
  (doseq [{k 0 :as component} draws
          :when component]
    (apply (draw-fns k) graphics (rest component))))

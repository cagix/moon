(ns cdq.create.graphics.draw-fns
  (:require cdq.create.graphics.draw.with-line-width
            cdq.create.graphics.draw.grid
            cdq.create.graphics.draw.texture-region
            cdq.create.graphics.draw.text
            cdq.create.graphics.draw.ellipse
            cdq.create.graphics.draw.filled-ellipse
            cdq.create.graphics.draw.circle
            cdq.create.graphics.draw.filled-circle
            cdq.create.graphics.draw.rectangle
            cdq.create.graphics.draw.filled-rectangle
            cdq.create.graphics.draw.arc
            cdq.create.graphics.draw.sector
            cdq.create.graphics.draw.line))

(def k->fn
  {:draw/with-line-width  cdq.create.graphics.draw.with-line-width/do!
   :draw/grid             cdq.create.graphics.draw.grid/do!
   :draw/texture-region   cdq.create.graphics.draw.texture-region/do!
   :draw/text             cdq.create.graphics.draw.text/do!
   :draw/ellipse          cdq.create.graphics.draw.ellipse/do!
   :draw/filled-ellipse   cdq.create.graphics.draw.filled-ellipse/do!
   :draw/circle           cdq.create.graphics.draw.circle/do!
   :draw/filled-circle    cdq.create.graphics.draw.filled-circle/do!
   :draw/rectangle        cdq.create.graphics.draw.rectangle/do!
   :draw/filled-rectangle cdq.create.graphics.draw.filled-rectangle/do!
   :draw/arc              cdq.create.graphics.draw.arc/do!
   :draw/sector           cdq.create.graphics.draw.sector/do!
   :draw/line             cdq.create.graphics.draw.line/do!})

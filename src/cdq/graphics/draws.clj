(ns cdq.graphics.draws
  (:require #_[clojure.tx-handler :as tx-handler]
            [clojure.gdx.shape-drawer :as sd]
            [cdq.graphics.draw.arc]
            [cdq.graphics.draw.circle]
            [cdq.graphics.draw.ellipse]
            [cdq.graphics.draw.filled-circle]
            [cdq.graphics.draw.filled-ellipse]
            [cdq.graphics.draw.filled-rectangle]
            [cdq.graphics.draw.line]
            [cdq.graphics.draw.rectangle]
            [cdq.graphics.draw.sector]
            [cdq.graphics.draw.text]
            [cdq.graphics.draw.texture-region]))

(declare handle!)

(def ^:private draw-fns
  {:draw/with-line-width  (fn
                            [{:keys [graphics/shape-drawer]
                              :as graphics}
                             width
                             draws]
                            (sd/with-line-width shape-drawer width
                              (handle! graphics draws)))

   :draw/grid             (fn do!
                            [graphics leftx bottomy gridw gridh cellw cellh color]
                            (let [w (* (float gridw) (float cellw))
                                  h (* (float gridh) (float cellh))
                                  topy (+ (float bottomy) (float h))
                                  rightx (+ (float leftx) (float w))]
                              (doseq [idx (range (inc (float gridw)))
                                      :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
                                (handle! graphics
                                         [[:draw/line [linex topy] [linex bottomy] color]]))
                              (doseq [idx (range (inc (float gridh)))
                                      :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
                                (handle! graphics
                                         [[:draw/line [leftx liney] [rightx liney] color]]))))
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
  #_(tx-handler/actions! draw-fns graphics draws :strict? true)
  (doseq [{k 0 :as component} draws
          :when component]
    (apply (draw-fns k) graphics (rest component))))

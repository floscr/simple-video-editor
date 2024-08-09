(ns app.components.editor.views
  (:require
   [app.utils.css.core :refer-macros [css]]
   [app.bindings.dnd-kit.core :as dnd]
   [uix.core :as uix :refer [$ defui]]))

;; Styles ----------------------------------------------------------------------

(css wrapper-css []
  {:position "absolute"
   :inset 0
   :display "flex"
   :flex-direction "column"
   :align-items "center"
   :justify-content "center"
   :background "oklch(90% 0 0)"})

(css video-css []
  {:display "block"
   :width "100%"
   :height "auto"
   :max-width "600px"})

(css video-wrapper-css []
  {:position "relative"})

(css cropper-css []
  {:position "absolute"
   :inset 0
   :border "1px solid oklch(100% 0 0)"
   "--border-color" "oklch(0% 0 0 / 0.5)"
   :box-shadow "0 0 0 1px var(--border-color), inset 0 0 0 1px var(--border-color)"})

(css cropper-handle-css [k]
  {"--size" "10px"
   "--offset" "calc(var(--size) / 2)"
   :position "absolute"
   :background "red"
   :width "var(--size)"
   :aspect-ratio "1 / 1"
   :border-radius "99999px"})

;; Component -------------------------------------------------------------------

(defn Cropper [{:keys [children]}]
  ($ :div {:class (cropper-css)}
     (for [k [:top :right :bottom :left]]
       ($ :div {:style (let [size-offset "calc(var(--offset) * -1)"
                             center-offset "calc(50% - var(--offset))"]
                           (case k
                             :top {:top size-offset
                                   :left center-offset}
                             :right {:top center-offset
                                     :right size-offset}
                             :left {:left size-offset
                                    :top center-offset}
                             :bottom {:bottom size-offset
                                      :left center-offset}))
                :class (cropper-handle-css k)}))))

(defui Editor []
  (let [[video-url set-video-url!] (uix/use-state nil)
        [crop set-crop!] (uix/use-state #js {:x 0 :y 0})
        [zoom set-zoom!] (uix/use-state 1)]
    ($ :div {:class (wrapper-css)}
       ($ :input
          {:type "file"
           :accept "video/*"
           :on-change (fn [e]
                        (let [file (-> e .-target .-files (aget 0))
                              url (js/URL.createObjectURL file)]
                          (set-video-url! url)))})
       (when video-url
         ($ :div {:class (video-wrapper-css)}
            ($ Cropper)
            ($ :video
               {:class [(video-css)]
                :src video-url}))))))

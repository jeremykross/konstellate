(ns konstellate.style
  (:require
    garden.selectors
    [garden.core :as garden]
    [garden.def :refer [defkeyframes]]
    [konstellate.editor.style :as editor]
    [konstellate.graffle.style :as graffle]
    [konstellate.components.style :as components]))

(def shadow-color "rgba(46, 91, 255, 0.4)")
(def shadow "0 5px 25px 0 rgba(0, 0, 0, 0.6)")
(def left-shadow "5px 0px 25px 0 rgba(8, 16, 24, 0.3)")
(def bottom-shadow "0 2px 12px 0 rgba(8, 16, 24, 0.5)")
(def right-shadow "0 5px 25px 0 rgba(8, 16, 24, 0.3)")

(def primary "#FEA7BD")
(def secondary "#09EDC8")
(def grey "#f4f6fc")
(def text "#081018")

(def border "#d9e1ff")
(def base-dark "#141d26")
(def black "#081018")
(def highlight "#00a2ff")
(def light-text "#b0bac9")
(def border "#212b35")

(def background-gradient "linear-gradient(170deg, #212b35, #081018)")

(defkeyframes FadeInAnim
  [:from {:opacity 0
          :transform "scale(0.95, 0.95)"}]
  [:to {:opacity 1
        :transform "scale(1, 1)"}])

(defkeyframes FadeUp
  [:from {:opacity 0
          :transform "translateY(32px)"}]
  [:to {:opacity 1
        :transform "translateY(0)"}])

(def Reset
  [[:html :body {:width "100%"
                 :height "100%"}]
   [:body {:background-image background-gradient
           :box-sizing "border-box"
           :color text
           :font-style "sans-serif"
           :font-family "'Rubik', sans-serif"
           :margin 0
           :padding 0
           :width "100%"
           :height "100%"}]
   [:h1 {:font-size "32px"
         :font-weight "normal"
         :letter-spacing "4px"}]
   [:#app {:width "100%"
           :height "100%"}]
   [:body [:* {:box-sizing "border-box"
               :margin 0
               :padding 0}]]])

(def Main [:.main {:display "flex"
                   :flex-direction "column"
                   :overflow "hidden"
                   :position "relative"
                   :width "100%"
                   :height "100%"}
           [:.main-content {:display "flex"
                            :flex 1
                            :position "relative"}
            [:.workspaces {:display "flex"
                           :flex-direction "column"
                           :height "100%"}
             [:icon {:font-size "18px"
                     :margin-right "8px"}]
             [:.add-workspace {:background base-dark
                               :cursor "pointer"
                               :display "flex"
                               :align-items "center"
                               :font-size "14px"
                               :padding "16px 0 16px 29px"
                               :text-transform "uppercase"
                               :white-space "nowrap"}
              [:icon {:display "block"
                      :margin-left "auto"
                      :margin-right "24px"}]]]
            [:.graffle {:animation "FadeUp 500ms ease"
                        :box-shadow left-shadow
                        :flex 1
                        :position "relative"
                        :z-index 2}
             [:.workspace-title {:color "white"
                                 :font-size "20px"
                                 :left "32px"
                                 :position "absolute"
                                 :top "32px"
                                 :text-shadow "2px 2px 4px rgba(0,0,0,0.5)"}]]]])


(def AddResource
  [:.add-resource {:background (str highlight " !important")
                   :bottom "40px !important"
                   :right "40px !important"
                   :transition "transform 500ms ease"
                   :z-index 5}
   [:&.panel-open {:transform "translateX(-360px)"}]])

(def TitleBar
  [:.title-bar {:align-items "center"
                :background base-dark
                :box-shadow bottom-shadow
                :color "white"
                :display "flex"
                :height "48px"
                :padding "32px"
                :position "relative"
                :z-index 20}

   [:.import {:cursor "pointer"
              :flex 1
              :font-size "16px"
              :margin-right "56px"
              :letter-spacing "2px"
              :text-align "right"
              :text-transform "uppercase"}]
              

   [:.button {:height "48px"
              :line-height "48px"}]
   [".button > img" {:margin-left "8px"}]


   [:.workspace {:cursor "pointer"
                 :font-weight "bold"
                 :position "absolute"
                 :text-align "center"
                 :width "100%"}]

   [:.more {:background black
            :border "none"
            :cursor "pointer"
            :display "block"
            :font-weight "normal"
            :font-size "14px"
            :letter-spacing "2px"
            :margin-left "auto"
            :text-transform "uppercase"}]])
                

(def FloatingMenu
  [:.floating-menu {:background "white"
                    :border (str "1px solid " border)
                    :border-radius "8px"
                    :box-shadow shadow
                    :color text
                    :display "none"
                    :font-size "12px"
                    :font-weight "bold"
                    :opacity 0
                    :padding "16px 32px"
                    :position "absolute"
                    :min-width "128px"
                    :perspective "100px"
                    :text-transform "uppercase"
                    :letter-spacing "2px"
                    :transform-origin "50% 0"
                    :transition "transform 300ms ease, opacity 300ms ease"
                    :z-index 20}
   [:&.open {:display "block"
             :opacity 1}]
   [:ol {:list-style-type "none"}
    [:li {:cursor "pointer"
          :padding "16px 0"}
     [:&:hover {:color highlight}]]]])

(def SidePanel
  [:.side-panel {:background black
                 :color "white"
                 :height "100%"
                 :overflow "hidden"
                 :position "relative"
                 :width "64px"
                 :transition "width 500ms ease"}

   [:&.dark {:background "black"
             :color "white"}]

   [:&.open {:width "320px"}
    [(garden.selectors/> "" :.side-panel-content) {:opacity 1}]]

   [:h4 {:letter-spacing "1px"
         :margin "24px"
         :font-weight "normal"
         :text-transform "uppercase"}]

   [:.side-panel-content {:height "100%"
                          :max-height "100%"
                          :min-width "320px"
                          :opacity 0
                          :width "100%"
                          :transition "opacity 500ms ease"}]
   [:.open-arrow {:cursor "pointer"
                  :position "absolute"
                  :font-size "14px"
                  :right "27px"
                  :text-align "center"
                  :top "26px"
                  :transform-origin "50% 50%"
                  :transition "transform 500ms ease"
                  :transform "rotate(-180deg)"
                  :width "14px"
                  :z-index "2"}
    [:&.open {:transform "rotate(0)"}]]])

(def InfoPanel
  [:.info-panel {:background "rgba(0,0,0,0.72)"
                 :bottom 0
                 :border-radius "8px"
                 :box-shadow shadow
                 :color light-text
                 :font-size "14px"
                 :margin "32px"
                 :position "absolute"
                 :width "320px"
                 :top 0
                 :right 0
                 :transform "translateX(400px)"
                 :transition "transform 500ms ease"
                 :z-index 10}

   [:.heading {:border-bottom (str "1px solid " border)
               :align-items "center"
               :justify-content "center"
               :display "flex"
               :padding "24px"}
    [:.edit-resource {:color highlight
                      :cursor "pointer"
                      :font-weight "bold"
                      :margin-left "auto"
                      :text-transform "uppercase"}]]

   [:h3 {:color "white"
         :font-size "16px"}]

   [:h4 {:font-size "12px"
         :font-weight "bold"
         :letter-spacing "2px"
         :margin-top "16px"
         :text-transform "uppercase"}
    [:&:first-child {:margin-top 0}]]

   [:.info {:display "grid"
            :grid-gap "8px"
            :margin "24px"
            :white-space "pre-wrap"}]

   [:.resource {:white-space "pre-wrap"}]
   [:.button {:margin-bottom "16px"}]
   [:&.open {:right "0px"
             :transform "translateX(0)"}]])


(def WorkspaceLabel
  [:.workspace-label {:border-top (str "1px solid " border)
                      :cursor "pointer"
                      :padding "16px"
                      :position "relative"}
   [:&:last-child {:border-bottom (str "1px solid " border)}]
   [".workspace-label-content:hover .more" {:display "block"}]
   [:.workspace-label-content {:font-size "14px"
                               :position "relative"
                               :top "0"
                               :line-height "1.5em"
                               :transition "top 500ms ease"
                               :z-index 1}
    [:&.open {}
     [:.label-open-arrow {:transform "rotate(0)"}]
     [:.label-standard-content
      [:.inner {:display "grid"}]]]

    [:.label-open-arrow {:transition "transform 500ms ease"
                         :transform "rotate(-90deg)"}]

    [:.floating-menu {:top "32px"
                      :right "8px"}]
    [:.label-standard-content {:overflow "hidden"}
     [:.outer {:display "flex"}]
     [:.inner {:display "grid"
               :margin-left "19px"
               :height "0"
               :transition "height 500ms ease"}
      [:.workspace-label-resource {:color "white"
                                   :padding-top "16px"
                                   :white-space "nowrap"}
       [:&.selected {:color highlight}
        [:.dot {:background highlight}]]
       [:.dot {:background "white"
               :border-radius "50%"
               :display "inline-block"
               :height "8px"
               :margin-right "16px"
               :position "relative"
               :top "-2px"
               :vertical-align "middle"
               :width "8px"}]]]]
    [:.label-edit-content {:position "relative"}
     [:.enter-to-continue {:color highlight
                           :font-size "12px"
                           :letter-spacing "2px"
                           :right "16px"
                           :top "8px"
                           :position "absolute"
                           :text-transform "uppercase"}]]
    [:&.selected {}]
    [:.the-name {:flex 1
                 :margin-left "8px"}]]])

(def WorkspaceList 
  [:.workload-panel 
   {:display "flex"
    :flex-direction "column"
    :flex 1
    :overflow-y "auto"}])

(def styles
  [FadeInAnim
   FadeUp
   components/ActionButton
   components/Button
   components/TextInput
   graffle/Main
   graffle/Node
   AddResource
   FloatingMenu
   Main
   TitleBar
   Reset
   WorkspaceLabel
   WorkspaceList
   InfoPanel
   SidePanel])

(defn spit-styles!
  []
  (spit "resources/public/css/style.css" (garden/css (concat [Reset] styles editor/styles))))


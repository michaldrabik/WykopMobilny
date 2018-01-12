package io.github.feelfreelinux.wykopmobilny.ui.widgets.link.comment

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import io.github.feelfreelinux.wykopmobilny.R
import io.github.feelfreelinux.wykopmobilny.models.dataclass.LinkComment
import io.github.feelfreelinux.wykopmobilny.models.pojo.apiv2.models.LinkVoteResponse
import io.github.feelfreelinux.wykopmobilny.ui.dialogs.showExceptionDialog
import io.github.feelfreelinux.wykopmobilny.ui.modules.NewNavigatorApi
import io.github.feelfreelinux.wykopmobilny.utils.SettingsPreferencesApi
import io.github.feelfreelinux.wykopmobilny.utils.getActivityContext
import io.github.feelfreelinux.wykopmobilny.utils.isVisible
import io.github.feelfreelinux.wykopmobilny.utils.printout
import io.github.feelfreelinux.wykopmobilny.utils.textview.URLClickedListener
import io.github.feelfreelinux.wykopmobilny.utils.textview.prepareBody
import io.github.feelfreelinux.wykopmobilny.utils.usermanager.UserManagerApi
import kotlinx.android.synthetic.main.link_comment_layout.view.*
import kotlin.math.absoluteValue

class LinkCommentWidget(context: Context, attrs: AttributeSet) : CardView(context, attrs), URLClickedListener, LinkCommentView {
    lateinit var comment : LinkComment
    lateinit var presenter : LinkCommentPresenter
    lateinit var settingsApi : SettingsPreferencesApi
    lateinit var userManagerApi : UserManagerApi
    init {
        View.inflate(context, R.layout.link_comment_layout, this)
        isClickable = true
        isFocusable = true
        val typedValue = TypedValue()
        getActivityContext()!!.theme?.resolveAttribute(R.attr.cardviewStatelist, typedValue, true)
        setBackgroundResource(typedValue.resourceId)
    }


    fun setLinkCommentData(linkComment: LinkComment, linkPresenter: LinkCommentPresenter, userManager: UserManagerApi, settingsPreferencesApi: SettingsPreferencesApi) {
        comment = linkComment
        userManagerApi = userManager
        presenter = linkPresenter
        presenter.subscribe(this)
        presenter.linkId = linkComment.id
        settingsApi = settingsPreferencesApi
        setupHeader()
        setupBody()
        setupButtons()
    }

    private fun setupHeader() {
        authorHeaderView.setAuthorData(comment.author, comment.date, comment.app)
    }

    private fun setupButtons() {
        plusButton.setup(userManagerApi)
        plusButton.text = comment.voteCountPlus.toString()
        plusButton.voteListener = {
            presenter.voteUp()
            comment.userVote = 1
            minusButton.isButtonSelected = false
            minusButton.isEnabled = false
        }

        minusButton.setup(userManagerApi)
        minusButton.text = comment.voteCountMinus.absoluteValue.toString()
        minusButton.voteListener = {
            presenter.voteDown()
            comment.userVote = -1
            plusButton.isButtonSelected = false
            plusButton.isEnabled = false
        }

        when (comment.userVote) {
            -1 -> {
                minusButton.isButtonSelected = true
                plusButton.isButtonSelected = false
                minusButton.isEnabled = false
                plusButton.isEnabled = true
            }

            1 -> {
                plusButton.isButtonSelected = true
                minusButton.isButtonSelected = false
                plusButton.isEnabled = false
                minusButton.isEnabled = true
            }

            0 -> {
                plusButton.isButtonSelected = false
                minusButton.isButtonSelected = false
                plusButton.isEnabled = true
                minusButton.isEnabled = true
            }
        }
    }

    private fun setupBody() {
        commentImageView.setEmbed(comment.embed, settingsApi, presenter.newNavigatorApi)
        comment.body?.let {
            commentContentTextView.prepareBody(comment.body!!, this)
        }
        val margin = if (comment.id != comment.parentId) 8f else 0f
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, margin, resources.displayMetrics)
        val params = layoutParams as MarginLayoutParams
        params.setMargins(px.toInt(), params.topMargin, params.rightMargin, params.bottomMargin)
        requestLayout()

    }

    fun setStyleForComment(isAuthorComment: Boolean, commentId : Int = -1) {
        val credentials = userManagerApi.getUserCredentials()
        if (credentials != null && credentials.login == comment.author.nick) {
            authorBadgeStrip.isVisible = true
            authorBadgeStrip.setBackgroundColor(ContextCompat.getColor(context, R.color.colorBadgeOwn))
        } else if (isAuthorComment) {
            authorBadgeStrip.isVisible = true
            authorBadgeStrip.setBackgroundColor(ContextCompat.getColor(context, R.color.colorBadgeAuthors))
        } else {
            authorBadgeStrip.isVisible = false
        }

        if (commentId == comment.id) {
            authorBadgeStrip.isVisible = true
            authorBadgeStrip.setBackgroundColor(ContextCompat.getColor(context, R.color.plusPressedColor))
        }

    }

    override fun handleUrl(url: String) {
        presenter.handleUrl(url)
    }

    override fun setVoteCount(voteResponse: LinkVoteResponse) {
        comment.voteCount = voteResponse.voteCount
        comment.voteCountMinus = voteResponse.voteCountMinus
        comment.voteCountPlus = voteResponse.voteCountPlus
        plusButton.voteCount = voteResponse.voteCountPlus
        minusButton.voteCount = voteResponse.voteCountMinus.absoluteValue
    }

    override fun markVotedMinus() {
        minusButton.isEnabled = false
        plusButton.isEnabled = true
        minusButton.isButtonSelected = true
        plusButton.isButtonSelected = false
    }

    override fun markVotedPlus() {
        minusButton.isEnabled = true
        plusButton.isEnabled = false
        minusButton.isButtonSelected = false
        plusButton.isButtonSelected = true
    }

    override fun showErrorDialog(e: Throwable) {
        context.showExceptionDialog(e)
    }
}